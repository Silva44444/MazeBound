package net.mazebound.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TicketLifecycleManager extends ListenerAdapter {

    private static final long DELETE_DELAY_MS =
            TimeUnit.MINUTES.toMillis(30);

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "ticket-deletion-scheduler");
                thread.setDaemon(true);
                return thread;
            });

    // Channel ID -> scheduled deletion timestamp
    private final Map<String, Long> deletionTimes =
            new ConcurrentHashMap<>();

    // Channel ID -> scheduled task
    private final Map<String, ScheduledFuture<?>> scheduledTasks =
            new ConcurrentHashMap<>();

    private final Object persistenceLock = new Object();

    private final Path dataFile = findDataFile();

    private volatile JDA jda;

    private static Path findDataFile() {
        Path projectData = Path.of("DiscordBot", "data");
        Path localData = Path.of("data");

        Path directory = Files.isDirectory(Path.of("DiscordBot"))
                ? projectData
                : localData;

        return directory.resolve("closed-tickets.json");
    }

    /**
     * Called when the bot connects and is ready.
     * Restores deletion schedules saved before a restart.
     */
    @Override
    public void onReady(ReadyEvent event) {
        this.jda = event.getJDA();
        restoreSchedules();
    }

    /**
     * Schedule ticket deletion 30 minutes from now.
     *
     * Call this ONLY after the transcript has been uploaded
     * successfully and the ticket has been closed.
     */
    public boolean scheduleDeletion(TextChannel channel) {
        if (channel == null) {
            return false;
        }

        String channelId = channel.getId();
        long deleteAt = System.currentTimeMillis() + DELETE_DELAY_MS;

        synchronized (persistenceLock) {
            Long previousTime = deletionTimes.put(channelId, deleteAt);

            if (!saveSchedules()) {
                if (previousTime == null) {
                    deletionTimes.remove(channelId);
                } else {
                    deletionTimes.put(channelId, previousTime);
                }

                return false;
            }

            scheduleTask(channelId, deleteAt);
        }

        return true;
    }

    /**
     * Cancel a pending deletion when a ticket is reopened.
     */
    public boolean cancelDeletion(String channelId) {
        synchronized (persistenceLock) {
            Long previousTime = deletionTimes.remove(channelId);

            if (previousTime == null) {
                return true;
            }

            ScheduledFuture<?> task = scheduledTasks.remove(channelId);

            if (task != null) {
                task.cancel(false);
            }

            if (!saveSchedules()) {
                // Restore the entry if persistence fails.
                deletionTimes.put(channelId, previousTime);
                scheduleTask(channelId, previousTime);
                return false;
            }

            return true;
        }
    }

    private void scheduleTask(String channelId, long deleteAt) {
        ScheduledFuture<?> previous = scheduledTasks.remove(channelId);

        if (previous != null) {
            previous.cancel(false);
        }

        long delay = Math.max(0L, deleteAt - System.currentTimeMillis());

        ScheduledFuture<?> task = SCHEDULER.schedule(
                () -> deleteIfStillScheduled(channelId, deleteAt),
                delay,
                TimeUnit.MILLISECONDS
        );

        scheduledTasks.put(channelId, task);
    }

    private void deleteIfStillScheduled(String channelId, long expectedTime) {
        JDA currentJda = jda;

        if (currentJda == null) {
            return;
        }

        // A reopened ticket or a changed schedule must not be deleted.
        Long actualTime = deletionTimes.get(channelId);

        if (actualTime == null || actualTime != expectedTime) {
            return;
        }

        TextChannel channel = currentJda.getTextChannelById(channelId);

        if (channel == null) {
            removeSchedule(channelId);
            return;
        }

        // Only delete tickets that are still marked closed.
        String topic = channel.getTopic();

        if (topic == null || !topic.matches("(?s).*\\bstatus=closed\\b.*")) {
            cancelDeletion(channelId);
            return;
        }

        channel.delete().queue(
    ignored -> removeSchedule(channelId),
    error -> {
        // Retry after one minute if deletion fails.
        // The saved schedule remains in place.
        if (deletionTimes.get(channelId) != null
                && deletionTimes.get(channelId) == expectedTime) {
            ScheduledFuture<?> retry = SCHEDULER.schedule(
                () -> deleteIfStillScheduled(channelId, expectedTime),
                1,
                TimeUnit.MINUTES
            );

            scheduledTasks.put(channelId, retry);
        }
    }
);
    }

    private void removeSchedule(String channelId) {
        synchronized (persistenceLock) {
            deletionTimes.remove(channelId);
            scheduledTasks.remove(channelId);
            saveSchedules();
        }
    }

    private void restoreSchedules() {
        synchronized (persistenceLock) {
            try {
                if (!Files.exists(dataFile)) {
                    return;
                }

                String json = Files.readString(
                        dataFile,
                        StandardCharsets.UTF_8
                );

                Map<String, Long> saved = GSON.fromJson(
                        json,
                        new com.google.gson.reflect.TypeToken<
                                HashMap<String, Long>>() {}.getType()
                );

                if (saved == null) {
                    return;
                }

                deletionTimes.clear();
                deletionTimes.putAll(saved);

                for (Map.Entry<String, Long> entry
                        : deletionTimes.entrySet()) {
                    scheduleTask(entry.getKey(), entry.getValue());
                }

            } catch (Exception error) {
                System.err.println(
                        "Could not restore ticket deletion schedules: "
                                + error.getMessage()
                );
            }
        }
    }

    private boolean saveSchedules() {
        try {
            Files.createDirectories(dataFile.getParent());

            Path temporaryFile = dataFile.resolveSibling(
                    dataFile.getFileName() + ".tmp"
            );

            Files.writeString(
                    temporaryFile,
                    GSON.toJson(new HashMap<>(deletionTimes)),
                    StandardCharsets.UTF_8
            );

            try {
                Files.move(
                        temporaryFile,
                        dataFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (IOException atomicMoveError) {
                Files.move(
                        temporaryFile,
                        dataFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return true;

        } catch (IOException error) {
            System.err.println(
                    "Could not save ticket deletion schedules: "
                            + error.getMessage()
            );
            return false;
        }
    }
}