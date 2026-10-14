package net.mazebound.bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.function.Consumer;

public final class ApplicationTickets extends ListenerAdapter {

    private static final String PANEL_CHANNEL_ID = "1559180908391833600";
    private static final String TICKET_CATEGORY_ID = "1545476078108155904";
    private static final String STAFF_ROLE_ID = "1545478582141452298";
    private static final String HEAD_MANAGEMENT_ROLE_ID = "1558875474858352720";
    private static final String TRANSCRIPT_CHANNEL_ID = "1559204979120537733";

    private static final Map<String, String> APPLICATION_TYPES = Map.of(
        "developer", "Developer",
        "moderator", "Moderator",
        "builder_lore", "Builder / Lore Writer"
    );

    private final TicketLifecycleManager ticketLifecycleManager;

public ApplicationTickets(TicketLifecycleManager ticketLifecycleManager) {
    this.ticketLifecycleManager = ticketLifecycleManager;
}

    // Prevents one user from opening multiple tickets while this bot is running.
    private final Set<String> applicants = ConcurrentHashMap.newKeySet();

    public void postPanel(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null
                || event.getMember() == null
                || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You need administrator permissions to post the ticket panel.")
                .setEphemeral(true)
                .queue();
            return;
        }

        TextChannel panel = event.getJDA().getTextChannelById(PANEL_CHANNEL_ID);

        if (panel == null) {
            event.reply("I couldn't find the ticket panel channel.")
                .setEphemeral(true)
                .queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setColor(new Color(245, 130, 32))
            .setTitle("🗝️ Mazebound Support Center")
            .setDescription(
                "Choose the type of ticket you want to open.\n\n"
                + "🛠️ **Support** — Get help with Mazebound.\n"
                + "🚩 **Player Report** — Report a player privately to Head Management.\n"
                + "🔒 **Head Management Report** — Report a staff member privately to Head Management.\n"
                + "📋 **Application** — Apply to join the team.\n\n"
                + "Please open only one ticket at a time and provide accurate information."
            )
            .setFooter("Mazebound Ticket Center");

        StringSelectMenu menu = StringSelectMenu.create("ticket_type")
            .setPlaceholder("Choose a ticket type...")
            .addOption("Support", "support", "Get help from the staff team")
            .addOption("Player Report", "player_report", "Report a player to Head Management")
            .addOption("Head Management Report", "management_report",
                "Report a staff member privately to Head Management")
            .addOption("Application", "application", "Apply to join the team")
            .build();

        panel.sendMessageEmbeds(embed.build())
            .addActionRow(menu)
            .queue(
                success -> event.reply("Ticket center posted.")
                    .setEphemeral(true)
                    .queue(),
                error -> event.reply("Couldn't post the ticket center. Check the bot's permissions.")
                    .setEphemeral(true)
                    .queue()
            );
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String menuId = event.getComponentId();

        if ("ticket_type".equals(menuId)) {
            handleTicketType(event);
            return;
        }

        if ("application_role".equals(menuId)) {
            handleApplicationRole(event);
        }
    }

    private void handleTicketType(StringSelectInteractionEvent event) {
        if (event.getGuild() == null || event.getMember() == null) {
            event.reply("Tickets can only be opened inside the server.")
                .setEphemeral(true)
                .queue();
            return;
        }

        String choice = event.getValues().get(0);

        if ("application".equals(choice)) {
            StringSelectMenu menu = StringSelectMenu.create("application_role")
                .setPlaceholder("Choose the role you want to apply for...")
                .addOption("Developer", "developer", "Plugins, Java and technical work")
                .addOption("Moderator", "moderator", "Community safety and support")
                .addOption("Builder / Lore Writer", "builder_lore",
                    "World-building, structures and storytelling")
                .build();

            event.reply("Choose the role you want to apply for.")
                .addActionRow(menu)
                .setEphemeral(true)
                .queue();
            return;
        }

        String type;
        switch (choice) {
            case "support" -> type = "support";
            case "player_report" -> type = "player_report";
            case "management_report" -> type = "management_report";
            default -> {
                event.reply("That ticket type isn't recognized.")
                    .setEphemeral(true)
                    .queue();
                return;
            }
        }

        createTicket(event, type, null);
    }

    private void handleApplicationRole(StringSelectInteractionEvent event) {
        if (event.getGuild() == null || event.getMember() == null) {
            event.reply("Applications can only be opened inside the server.")
                .setEphemeral(true)
                .queue();
            return;
        }

        String selected = event.getValues().get(0);
        String applicationType = APPLICATION_TYPES.get(selected);

        if (applicationType == null) {
            event.reply("That application type isn't recognized.")
                .setEphemeral(true)
                .queue();
            return;
        }

        createTicket(event, "application", applicationType);
    }

    private void createTicket(
            StringSelectInteractionEvent event,
            String type,
            String applicationType) {

        String userId = event.getUser().getId();

        if (!applicants.add(userId)) {
            event.reply("You already have a ticket open. Please finish or close it first.")
                .setEphemeral(true)
                .queue();
            return;
        }

        Category category = event.getGuild().getCategoryById(TICKET_CATEGORY_ID);
        Role staffRole = event.getGuild().getRoleById(STAFF_ROLE_ID);
        Role managementRole = event.getGuild().getRoleById(HEAD_MANAGEMENT_ROLE_ID);

        if (category == null || staffRole == null || managementRole == null) {
            applicants.remove(userId);
            event.reply("Ticket setup is incomplete. Check the configured category and staff roles.")
                .setEphemeral(true)
                .queue();
            return;
        }

        boolean managementOnly =
            "player_report".equals(type) || "management_report".equals(type);

        if (managementOnly && "management_report".equals(type)
                && event.getMember().getRoles().contains(managementRole)) {
            applicants.remove(userId);
            event.reply("You can't submit a private report about Head Management through this panel.")
                .setEphemeral(true)
                .queue();
            return;
        }

        event.deferReply(true).queue();

        String prefix = switch (type) {
            case "support" -> "support";
            case "player_report" -> "player-report";
            case "management_report" -> "management-report";
            default -> "application";
        };

        String safeName = event.getUser().getName()
            .toLowerCase()
            .replaceAll("[^a-z0-9-]", "");

        if (safeName.isBlank()) {
            safeName = "user";
        }

        String channelName = prefix + "-" + safeName + "-"
            + userId.substring(Math.max(0, userId.length() - 4));

        String topic = "Mazebound ticket"
            + " | type=" + type
            + " | applicantId=" + userId
            + " | status=open"
            + " | claimedBy=none";

        event.getGuild().createTextChannel(channelName)
            .setParent(category)
            .setTopic(topic)
            .addPermissionOverride(
                event.getGuild().getPublicRole(),
                null,
                EnumSet.of(Permission.VIEW_CHANNEL)
            )
            .addPermissionOverride(
                event.getMember(),
                EnumSet.of(
                    Permission.VIEW_CHANNEL,
                    Permission.MESSAGE_SEND,
                    Permission.MESSAGE_HISTORY
                ),
                null
            )
           .queue(channel -> {
    if (managementOnly) {
        channel.getManager()
            .putPermissionOverride(
                staffRole,
                null,
                EnumSet.of(Permission.VIEW_CHANNEL)
            )
            .queue(
                ignored -> channel.getManager()
                    .putPermissionOverride(
                        managementRole,
                        EnumSet.of(
                            Permission.VIEW_CHANNEL,
                            Permission.MESSAGE_SEND,
                            Permission.MESSAGE_HISTORY
                        ),
                        null
                    )
                    .queue(
                        ignoredManagement -> finishTicketCreation(
                            event, channel, userId, type, applicationType,
                            staffRole, managementRole
                        ),
                        error -> {
                            applicants.remove(userId);
                            channel.delete().queue(
                                success -> event.getHook().editOriginal(
                                    "Couldn't configure private report permissions. No ticket was created."
                                ).queue(),
                                deleteError -> event.getHook().editOriginal(
                                    "Couldn't configure private report permissions. Please contact an administrator."
                                ).queue()
                            );
                        }
                    ),
                error -> {
                    applicants.remove(userId);
                    channel.delete().queue(
                        success -> event.getHook().editOriginal(
                            "Couldn't configure private report permissions. No ticket was created."
                        ).queue(),
                        deleteError -> event.getHook().editOriginal(
                            "Couldn't configure private report permissions. Please contact an administrator."
                        ).queue()
                    );
                }
            );
    } else {
        channel.getManager()
            .putPermissionOverride(
                staffRole,
                EnumSet.of(
                    Permission.VIEW_CHANNEL,
                    Permission.MESSAGE_SEND,
                    Permission.MESSAGE_HISTORY
                ),
                null
            )
            .queue(
                ignored -> finishTicketCreation(
                    event, channel, userId, type, applicationType,
                    staffRole, managementRole
                ),
                error -> {
                    applicants.remove(userId);
                    channel.delete().queue(
                        success -> event.getHook().editOriginal(
                            "Couldn't configure ticket permissions."
                        ).queue(),
                        deleteError -> event.getHook().editOriginal(
                            "Couldn't configure ticket permissions. Please contact an administrator."
                        ).queue()
                    );
                }
            );
    }
}, error -> {
    applicants.remove(userId);
    event.getHook().editOriginal(
        "Couldn't create your ticket. Please contact staff."
    ).queue();
});
    }

    private void createTicket(
            StringSelectInteractionEvent event,
            String type,
            String applicationType,
            boolean unused) {
        createTicket(event, type, applicationType);
    }

    private void finishTicketCreation(
            StringSelectInteractionEvent event,
            TextChannel channel,
            String userId,
            String type,
            String applicationType,
            Role staffRole,
            Role managementRole) {

        String intro = switch (type) {
            case "support" ->
                "Welcome to your **Support** ticket. Explain what you need help with, "
                + "and staff will respond when available.";
            case "player_report" ->
                "Welcome to your **Player Report** ticket. Describe what happened, "
                + "include relevant dates and evidence, and avoid sharing unrelated private information. "
                + "Only you and Head Management should be able to see this channel.";
            case "management_report" ->
                "Welcome to your **Head Management Report** ticket. Explain the concern and provide any relevant evidence. "
                + "This channel is intended to be private between you and Head Management.";
            default ->
                "Welcome to your **" + applicationType + " application**!\n\n"
                + questionsFor(applicationType);
        };

        String mention = "application".equals(type)
            ? "<@&" + STAFF_ROLE_ID + ">"
            : ("support".equals(type) ? "<@&" + STAFF_ROLE_ID + ">" : "<@&" + HEAD_MANAGEMENT_ROLE_ID + ">");

        channel.sendMessage(
            "<@" + userId + "> " + intro + "\n\n"
            + mention + " A new ticket is ready for review."
        ).addActionRow(
            Button.primary("ticket_claim", "Claim ticket"),
            Button.secondary("ticket_close", "Close ticket")
        ).queue(
            success -> {
                if ("application".equals(type)) {
                    channel.sendMessage("Application review actions:")
                        .addActionRow(
                            Button.success("app_decision:accept:" + userId, "Accept"),
                            Button.danger("app_decision:decline:" + userId, "Decline"),
                            Button.secondary("app_decision:moreinfo:" + userId, "Request More Info")
                        )
                        .queue();
                }

                event.getHook().editOriginal(
                    "Your private ticket is ready: " + channel.getAsMention()
                ).queue();
            },
            error -> event.getHook().editOriginal(
                "The ticket was created, but I couldn't post its introduction. Please notify staff."
            ).queue()
        );
    }

    private String questionsFor(String type) {
        return switch (type) {
            case "Developer" ->
                "**Developer application questions**\n"
                + "1. What programming languages do you know?\n"
                + "2. What Minecraft or Java projects have you built?\n"
                + "3. What would you like to contribute to Mazebound?\n"
                + "4. Can you explain and test the code you submit?";
            case "Moderator" ->
                "**Moderator application questions**\n"
                + "1. Why do you want to moderate Mazebound?\n"
                + "2. How would you handle an argument between members?\n"
                + "3. What would you do if a friend broke the rules?\n"
                + "4. How much time can you realistically contribute?";
            default ->
                "**Builder / Lore Writer application questions**\n"
                + "1. Which interests you more: building, lore, or both?\n"
                + "2. Describe something you've built or written.\n"
                + "3. How would your work fit Mazebound's fantasy setting?\n"
                + "4. What would you like to contribute first?";
        };
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if ("ticket_claim".equals(id)) {
            claimTicket(event);
            return;
        }

        if ("ticket_close".equals(id)) {
            closeTicket(event);
            return;
        }

        if (id.startsWith("app_decision:")) {
            handleDecision(event);
            return;
        }

        if (id.startsWith("applicant_close:")) {
            handleApplicantClose(event);
            return;
        }

        if (id.startsWith("applicant_keep:")) {
            handleApplicantKeep(event);
        }
    }

    private void claimTicket(ButtonInteractionEvent event) {
        if (event.getGuild() == null
                || event.getMember() == null
                || !(event.getChannel() instanceof TextChannel channel)
                || !isTicket(channel)) {
            event.reply("This isn't a recognized ticket channel.")
                .setEphemeral(true)
                .queue();
            return;
        }

       boolean headManagement = isStaff(
    event.getMember(),
    event.getGuild().getRoleById(HEAD_MANAGEMENT_ROLE_ID)
);

boolean generalStaff = isStaff(
    event.getMember(),
    event.getGuild().getRoleById(STAFF_ROLE_ID)
);

if (isManagementOnlyTicket(channel) ? !headManagement
        : !generalStaff && !headManagement) {
    event.reply("You don't have permission to claim this ticket.")
        .setEphemeral(true)
        .queue();
    return;
}

        String topic = channel.getTopic();
        if (topic == null) {
            event.reply("Couldn't read this ticket's metadata.")
                .setEphemeral(true)
                .queue();
            return;
        }

        if (topic.contains("claimedBy=") && !topic.contains("claimedBy=none")) {
            event.reply("This ticket has already been claimed.")
                .setEphemeral(true)
                .queue();
            return;
        }

        String updatedTopic = topic.replace("claimedBy=none",
            "claimedBy=" + event.getUser().getId());

        String newName = "claimed-by-" + event.getUser().getName()
            .toLowerCase()
            .replaceAll("[^a-z0-9-]", "");

        if (newName.length() > 90) {
            newName = newName.substring(0, 90);
        }

        event.deferReply(true).queue();

        channel.getManager()
            .setName(newName)
            .setTopic(updatedTopic)
            .queue(
                success -> event.getHook().editOriginal(
                    "Ticket claimed by " + event.getUser().getAsMention() + "."
                ).queue(),
                error -> event.getHook().editOriginal(
                    "Couldn't claim this ticket. Check the bot's Manage Channels permission."
                ).queue()
            );
    }

private void closeTicket(ButtonInteractionEvent event) {
    if (event.getGuild() == null
            || event.getMember() == null
            || !(event.getChannel() instanceof TextChannel channel)
            || !isTicket(channel)) {
        event.reply("This isn't a recognized ticket channel.")
            .setEphemeral(true)
            .queue();
        return;
    }

    if (isClosed(channel)) {
        event.reply("This ticket is already closed.")
            .setEphemeral(true)
            .queue();
        return;
    }

    String applicantId = extractApplicantId(channel);
    boolean owner = event.getUser().getId().equals(applicantId);

    boolean staff = isStaff(
        event.getMember(),
        event.getGuild().getRoleById(STAFF_ROLE_ID)
    ) || isStaff(
        event.getMember(),
        event.getGuild().getRoleById(HEAD_MANAGEMENT_ROLE_ID)
    );

  boolean headManagement = isStaff(
    event.getMember(),
    event.getGuild().getRoleById(HEAD_MANAGEMENT_ROLE_ID)
);

boolean generalStaff = isStaff(
    event.getMember(),
    event.getGuild().getRoleById(STAFF_ROLE_ID)
);

boolean authorized = isManagementOnlyTicket(channel)
    ? owner || headManagement
    : owner || generalStaff || headManagement;

if (!authorized) {
    event.reply(
        "Only the ticket owner or authorized staff can close this ticket."
    ).setEphemeral(true).queue();
    return;
}

    event.deferReply(true).queue();

    closeWithTranscript(
        channel,
        event.getUser().getAsMention(),
        "Ticket closed",
        message -> event.getHook().editOriginal(message).queue(),
        message -> event.getHook().editOriginal(message).queue()
    );
}

    private void handleDecision(ButtonInteractionEvent event) {
        if (event.getGuild() == null || event.getMember() == null) {
            event.reply("This action is only available in the server.")
                .setEphemeral(true)
                .queue();
            return;
        }

        Role staffRole = event.getGuild().getRoleById(STAFF_ROLE_ID);
        if (!isStaff(event.getMember(), staffRole)) {
            event.reply("Only authorized application reviewers can use these buttons.")
                .setEphemeral(true)
                .queue();
            return;
        }

        String[] parts = event.getComponentId().split(":");
        if (parts.length != 3) {
            event.reply("Invalid application action.")
                .setEphemeral(true)
                .queue();
            return;
        }

        String decision = parts[1];
        String applicantId = parts[2];

        switch (decision) {
            case "accept", "decline" -> {
                String result = decision.equals("accept") ? "accepted" : "declined";

                event.reply("Application marked as " + result + ".")
                    .setEphemeral(true)
                    .queue();

                event.getChannel().sendMessage(
                    "<@" + applicantId + "> — your application has been " + result
                    + ". You can close this ticket when you're ready."
                ).addActionRow(
                    Button.success("applicant_close:" + applicantId, "Close my ticket"),
                    Button.secondary("applicant_keep:" + applicantId, "Keep ticket open")
                ).queue();
            }
            case "moreinfo" -> event.reply(
                "Please ask <@" + applicantId
                    + "> for the additional information needed in this ticket."
            ).setEphemeral(true).queue();
            default -> event.reply("Unknown application decision.")
                .setEphemeral(true).queue();
        }
    }

private void handleApplicantClose(ButtonInteractionEvent event) {
    String applicantId = event.getComponentId()
        .substring("applicant_close:".length());

    if (!event.getUser().getId().equals(applicantId)) {
        event.reply("Only the applicant who opened this ticket can close it.")
            .setEphemeral(true).queue();
        return;
    }

    if (!(event.getChannel() instanceof TextChannel channel)
            || !isTicket(channel)
            || !applicantId.equals(extractApplicantId(channel))) {
        event.reply("This isn't a recognized ticket for your account.")
            .setEphemeral(true).queue();
        return;
    }

    if (isClosed(channel)) {
        event.reply("This ticket is already closed.")
            .setEphemeral(true).queue();
        return;
    }

    event.deferReply(true).queue();

    closeWithTranscript(
        channel,
        event.getUser().getAsMention(),
        "Closed by ticket owner",
        message -> event.getHook().editOriginal(message).queue(),
        message -> event.getHook().editOriginal(message).queue()
    );
}

    private void handleApplicantKeep(ButtonInteractionEvent event) {
        String applicantId = event.getComponentId()
            .substring("applicant_keep:".length());

        if (!event.getUser().getId().equals(applicantId)) {
            event.reply("Only the applicant who opened this ticket can choose this.")
                .setEphemeral(true)
                .queue();
            return;
        }

        if (!(event.getChannel() instanceof TextChannel channel)
                || !isTicket(channel)
                || !applicantId.equals(extractApplicantId(channel))) {
            event.reply("This isn't a recognized ticket for your account.")
                .setEphemeral(true)
                .queue();
            return;
        }

        if (isClosed(channel)) {
            event.reply("This ticket is already closed. Ask staff to reopen it.")
                .setEphemeral(true)
                .queue();
            return;
        }

        event.reply("No problem — the ticket will stay open.")
            .setEphemeral(true)
            .queue();
    }

public void forceClose(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null || event.getMember() == null) {
        event.reply("This command can only be used in the server.")
            .setEphemeral(true).queue();
        return;
    }

    var option = event.getOption("ticket");
    var reasonOption = event.getOption("reason");

    if (option == null || reasonOption == null
            || !(option.getAsChannel() instanceof TextChannel channel)
            || !isTicket(channel)) {
        event.reply("Select a recognized ticket channel.")
            .setEphemeral(true).queue();
        return;
    }

    boolean headManagement = isStaff(
        event.getMember(),
        event.getGuild().getRoleById(HEAD_MANAGEMENT_ROLE_ID)
    );

    boolean generalStaff = isStaff(
        event.getMember(),
        event.getGuild().getRoleById(STAFF_ROLE_ID)
    );

    if (isManagementOnlyTicket(channel) ? !headManagement
            : !generalStaff && !headManagement) {
        event.reply("You don't have permission to force-close this ticket.")
            .setEphemeral(true).queue();
        return;
    }

    if (isClosed(channel)) {
        event.reply("This ticket is already closed.")
            .setEphemeral(true).queue();
        return;
    }

    String reason = reasonOption.getAsString().trim();

    if (reason.isBlank()) {
        event.reply("A reason is required.")
            .setEphemeral(true).queue();
        return;
    }

    event.deferReply(true).queue();

    closeWithTranscript(
        channel,
        event.getUser().getAsMention(),
        reason,
        message -> event.getHook().editOriginal(
            "Closed " + channel.getAsMention() + ". " + message
        ).queue(),
        message -> event.getHook().editOriginal(message).queue()
    );
}

public void reopenTicket(SlashCommandInteractionEvent event) {
if (event.getGuild() == null || event.getMember() == null) {
event.reply("This command can only be used in the server.")
.setEphemeral(true)
.queue();
return;
}

var option = event.getOption("ticket");
if (option == null
        || !(option.getAsChannel() instanceof TextChannel channel)
        || !isTicket(channel)) {
    event.reply("Select a recognized ticket channel.")
        .setEphemeral(true)
        .queue();
    return;
}

boolean headManagement = isStaff(
    event.getMember(),
    event.getGuild().getRoleById(HEAD_MANAGEMENT_ROLE_ID)
);

boolean generalStaff = isStaff(
    event.getMember(),
    event.getGuild().getRoleById(STAFF_ROLE_ID)
);

if (isManagementOnlyTicket(channel) ? !headManagement
        : !generalStaff && !headManagement) {
    event.reply("You don't have permission to reopen this ticket.")
        .setEphemeral(true)
        .queue();
    return;
}

if (!isClosed(channel)) {
    event.reply("This ticket is already open.")
        .setEphemeral(true)
        .queue();
    return;
}

String applicantId = extractApplicantId(channel);
Member applicant = event.getGuild().getMemberById(applicantId);

if (applicant == null) {
    event.reply("The ticket owner isn't currently in the server.")
        .setEphemeral(true)
        .queue();
    return;
}

event.deferReply(true).queue();

channel.getManager()
    .putPermissionOverride(
        applicant,
        EnumSet.of(
            Permission.VIEW_CHANNEL,
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_HISTORY
        ),
        null
    )
    .setTopic(updateTopicStatus(channel, "open"))
    .queue(
        success -> {
            if (!ticketLifecycleManager.cancelDeletion(channel.getId())) {
                event.getHook().editOriginal(
                    "The ticket reopened, but the saved deletion schedule "
                    + "couldn't be updated. Contact an administrator."
                ).queue();
                return;
            }

            channel.sendMessage("🔓 This ticket has been reopened by staff.")
                .queue();

            event.getHook().editOriginal(
                "Reopened " + channel.getAsMention() + "."
            ).queue();
        },
        error -> event.getHook().editOriginal(
            "Couldn't reopen the ticket. Check the bot's permissions."
        ).queue()
    );

}

private void closeWithTranscript(
        TextChannel channel,
        String actorMention,
        String reason,
        Consumer<String> onSuccess,
        Consumer<String> onFailure
) {
    TextChannel transcriptChannel =
        channel.getJDA().getTextChannelById(TRANSCRIPT_CHANNEL_ID);

    if (transcriptChannel == null) {
        onFailure.accept(
            "Transcript channel not found. The ticket was left open."
        );
        return;
    }

    MessageHistory history = channel.getHistory();
    List<Message> messages = new ArrayList<>();

    retrieveTranscriptPage(
        channel,
        history,
        messages,
        transcriptChannel,
        actorMention,
        reason,
        onSuccess,
        onFailure
    );
}

private void retrieveTranscriptPage(
        TextChannel channel,
        MessageHistory history,
        List<Message> messages,
        TextChannel transcriptChannel,
        String actorMention,
        String reason,
        Consumer<String> onSuccess,
        Consumer<String> onFailure
) {
    history.retrievePast(100).queue(
        batch -> {
            messages.addAll(batch);

            if (batch.size() == 100) {
                retrieveTranscriptPage(
                    channel,
                    history,
                    messages,
                    transcriptChannel,
                    actorMention,
                    reason,
                    onSuccess,
                    onFailure
                );
            } else {
                uploadTranscript(
                    channel,
                    messages,
                    transcriptChannel,
                    actorMention,
                    reason,
                    onSuccess,
                    onFailure
                );
            }
        },
        error -> onFailure.accept(
            "Couldn't retrieve the ticket history. The ticket was left open."
        )
    );
}

private void uploadTranscript(
        TextChannel channel,
        List<Message> messages,
        TextChannel transcriptChannel,
        String actorMention,
        String reason,
        Consumer<String> onSuccess,
        Consumer<String> onFailure
) {
    Collections.reverse(messages);

    StringBuilder transcript = new StringBuilder();

    transcript.append("Mazebound Ticket Transcript\n")
        .append("Channel: ").append(channel.getName()).append("\n")
        .append("Channel ID: ").append(channel.getId()).append("\n")
        .append("Closed by: ").append(actorMention).append("\n")
        .append("Reason: ").append(reason).append("\n")
        .append("Generated: ").append(Instant.now()).append("\n")
        .append("=".repeat(60)).append("\n\n");

    for (Message message : messages) {
        transcript.append("[")
            .append(message.getTimeCreated())
            .append("] ")
            .append(message.getAuthor().getName())
            .append(" (ID: ")
            .append(message.getAuthor().getId())
            .append("):\n");

        String content = message.getContentDisplay();

        transcript.append(content.isBlank() ? "[No text content]" : content)
            .append("\n");

        message.getAttachments().forEach(attachment ->
            transcript.append("Attachment: ")
                .append(attachment.getUrl())
                .append("\n")
        );

        transcript.append("\n");
    }

    String timestamp = DateTimeFormatter
        .ofPattern("yyyyMMdd-HHmmss")
        .withZone(ZoneOffset.UTC)
        .format(Instant.now());

    String filename = "ticket-" + channel.getId()
        + "-" + timestamp + ".txt";

    byte[] data = transcript.toString()
        .getBytes(StandardCharsets.UTF_8);

    transcriptChannel.sendFiles(
        FileUpload.fromData(data, filename)
    ).queue(
        uploaded -> finishTicketClose(
            channel,
            actorMention,
            reason,
            onSuccess,
            onFailure
        ),
        error -> onFailure.accept(
            "Transcript upload failed. The ticket was left open."
        )
    );
}

private void finishTicketClose(
        TextChannel channel,
        String actorMention,
        String reason,
        Consumer<String> onSuccess,
        Consumer<String> onFailure
) {
    Member owner = channel.getGuild()
        .getMemberById(extractApplicantId(channel));

    var manager = channel.getManager()
        .setTopic(updateTopicStatus(channel, "closed"));

    if (owner != null) {
        manager = manager.putPermissionOverride(
            owner,
            null,
            EnumSet.of(
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND
            )
        );
    }

    manager.queue(
        success -> {
            if (!ticketLifecycleManager.scheduleDeletion(channel)) {
                onFailure.accept(
                    "Transcript saved and ticket closed, but the deletion "
                    + "schedule couldn't be saved. The ticket won't be "
                    + "automatically deleted."
                );
                return;
            }

            channel.sendMessage(
                "🔒 Ticket closed by " + actorMention
                + ". A transcript was saved. This ticket is scheduled "
                + "for deletion in 30 minutes unless staff reopen it."
                + (reason.isBlank() ? "" : "\nReason: " + reason)
            ).queue(
                ignored -> onSuccess.accept(
                    "Ticket closed. Transcript saved; deletion scheduled "
                    + "for 30 minutes from now."
                ),
                error -> onSuccess.accept(
                    "Ticket closed and transcript saved. Deletion is scheduled."
                )
            );
        },
        error -> onFailure.accept(
            "The transcript was saved, but the ticket couldn't be closed. "
            + "No deletion was scheduled."
        )
    );
}

    private boolean isStaff(Member member, Role role) {
        return role != null && member.getRoles().contains(role);
    }

   private boolean isManagementOnlyTicket(TextChannel channel) {
String topic = channel.getTopic();

return topic != null
    && (topic.contains("type=player_report")
        || topic.contains("type=management_report"));

}
   
    private boolean isTicket(TextChannel channel) {
        if (channel.getParentCategory() == null
                || !TICKET_CATEGORY_ID.equals(channel.getParentCategoryId())) {
            return false;
        }

        String topic = channel.getTopic();
        return topic != null && topic.contains("applicantId=");
    }

    private String extractApplicantId(TextChannel channel) {
        String topic = channel.getTopic();

        if (topic == null) {
            return "";
        }

        String marker = "applicantId=";
        int start = topic.indexOf(marker);

        if (start < 0) {
            return "";
        }

        String id = topic.substring(start + marker.length()).trim();
        int separator = id.indexOf('|');

        if (separator >= 0) {
            id = id.substring(0, separator).trim();
        }

        return id;
    }

    private boolean isClosed(TextChannel channel) {
        String topic = channel.getTopic();
        return topic != null && topic.contains("status=closed");
    }

    private String updateTopicStatus(TextChannel channel, String status) {
        String topic = channel.getTopic();

        if (topic == null) {
            return "Mazebound ticket | status=" + status;
        }

        String updated = topic.replaceAll("\\s*\\|\\s*status=(open|closed)", "");
        return updated + " | status=" + status;
    }
}