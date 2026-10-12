
package net.mazebound.bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ApplicationTickets extends ListenerAdapter {

    private static final String PANEL_CHANNEL_ID =
        "1559180908391833600";
    private static final String TICKET_CATEGORY_ID =
        "1545476078108155904";
    private static final String STAFF_ROLE_ID =
        "1545478582141452298";

    private static final Map<String, String> APPLICATION_TYPES =
        Map.of(
            "app_developer", "Developer",
            "app_moderator", "Moderator",
            "app_builder_lore", "Builder / Lore Writer"
        );

    // Tracks open tickets while the bot is running.
    private final Set<String> applicants = ConcurrentHashMap.newKeySet();

    public void postPanel(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null
                || event.getMember() == null
                || !event.getMember().hasPermission(
                    Permission.ADMINISTRATOR)) {
            event.reply("You need administrator permissions to post applications.")
                .setEphemeral(true).queue();
            return;
        }

        TextChannel panel = event.getJDA()
            .getTextChannelById(PANEL_CHANNEL_ID);

        if (panel == null) {
            event.reply("Application panel channel not found.")
                .setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setColor(new Color(245, 130, 32))
            .setTitle("🗝️ Join the Mazebound Team")
            .setDescription(
                "Want to help build Mazebound?\n\n"
                + "Choose the role you want to apply for below. "
                + "The bot will open a private ticket and ask "
                + "you a few questions.\n\n"
                + "**Developer** — plugins, Java and technical work\n"
                + "**Moderator** — community safety and support\n"
                + "**Builder / Lore Writer** — world-building, "
                + "structures and storytelling"
            )
            .setFooter("Please open only one application at a time.");

        panel.sendMessageEmbeds(embed.build())
            .addActionRow(
                Button.primary("app_developer", "Developer"),
                Button.primary("app_moderator", "Moderator"),
                Button.primary("app_builder_lore", "Builder / Lore Writer")
            )
            .queue(
                success -> event.reply("Application panel posted.")
                    .setEphemeral(true).queue(),
                error -> event.reply("Couldn't post the application panel.")
                    .setEphemeral(true).queue()
            );
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (APPLICATION_TYPES.containsKey(id)) {
            createTicket(event, APPLICATION_TYPES.get(id));
            return;
        }

        if (id.startsWith("app_decision:")) {
            handleDecision(event);
        }


if (id.startsWith("applicant_close:")) {
    handleApplicantClose(event);
    return;
}

if (id.startsWith("applicant_keep:")) {
    handleApplicantKeep(event);
}

    }

    private void createTicket(
            ButtonInteractionEvent event,
            String applicationType) {

        if (event.getGuild() == null || event.getMember() == null) {
            event.reply("Applications can only be opened in a server.")
                .setEphemeral(true).queue();
            return;
        }

        String userId = event.getUser().getId();

        if (!applicants.add(userId)) {
            event.reply("You already have an open application ticket.")
                .setEphemeral(true).queue();
            return;
        }

       Category category = event.getGuild()
    .getCategoryById(TICKET_CATEGORY_ID);

        Role staffRole = event.getGuild()
            .getRoleById(STAFF_ROLE_ID);

        if (category == null || staffRole == null) {
            applicants.remove(userId);
            event.reply("The ticket category or staff role could not be found.")
                .setEphemeral(true).queue();
            return;
        }

        Member applicant = event.getMember();

        event.deferReply(true).queue();

       event.getGuild().createTextChannel(
    "application-" + event.getUser().getName()
        .toLowerCase().replaceAll("[^a-z0-9-]", "")
        + "-" + userId.substring(userId.length() - 4))
   .setTopic("Mazebound application | applicantId=" + userId + " | status=open")
            .setParent(category)
            .addPermissionOverride(
                event.getGuild().getPublicRole(),
                null,
                EnumSet.of(Permission.VIEW_CHANNEL))
            .addPermissionOverride(
                applicant,
                EnumSet.of(
                    Permission.VIEW_CHANNEL,
                    Permission.MESSAGE_SEND,
                    Permission.MESSAGE_HISTORY),
                null)
            .addPermissionOverride(
                staffRole,
                EnumSet.of(
                    Permission.VIEW_CHANNEL,
                    Permission.MESSAGE_SEND,
                    Permission.MESSAGE_HISTORY),
                null)
            .queue(channel -> {
                channel.sendMessage(
                    applicant.getAsMention()
                    + " Welcome to your **" + applicationType
                    + " application**!\n\n"
                    + "Please answer the questions below in this channel. "
                    + "Staff will review your answers.\n\n"
                    + questionsFor(applicationType)
                    + "\n\n<@&" + STAFF_ROLE_ID
                    + "> A new application is ready for review."
                ).addActionRow(
                    Button.success(
                        "app_decision:accept:" + userId,
                        "Accept"),
                    Button.danger(
                        "app_decision:decline:" + userId,
                        "Decline"),
                    Button.secondary(
                        "app_decision:moreinfo:" + userId,
                        "Request More Info")
                ).queue();

                event.getHook().editOriginal(
                    "Your private application ticket is ready: "
                    + channel.getAsMention()
                ).queue();
            }, error -> {
                applicants.remove(userId);
                event.getHook().editOriginal(
                    "Couldn't create your ticket. Please contact staff."
                ).queue();
            });
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

   
private void handleDecision(ButtonInteractionEvent event) {
    if (event.getGuild() == null || event.getMember() == null) {
        event.reply("This action is only available in the server.")
            .setEphemeral(true).queue();
        return;
    }

    Role staffRole = event.getGuild().getRoleById(STAFF_ROLE_ID);

    if (staffRole == null
            || !event.getMember().getRoles().contains(staffRole)) {
        event.reply("Only authorized application reviewers can use these buttons.")
            .setEphemeral(true).queue();
        return;
    }

    String[] parts = event.getComponentId().split(":");

    if (parts.length != 3) {
        event.reply("Invalid application action.")
            .setEphemeral(true).queue();
        return;
    }

    String decision = parts[1];
    String applicantId = parts[2];

    switch (decision) {
        case "accept", "decline" -> {
            String result = decision.equals("accept")
                ? "accepted"
                : "declined";

            event.reply("Application marked as " + result
                    + ". The ticket will stay open until it is closed.")
                .setEphemeral(true).queue();

            event.getChannel().sendMessage(
                "<@" + applicantId + "> — your application has been "
                + result + ". You can close this ticket when you're ready, "
                + "or keep it open if you have questions."
            ).addActionRow(
                Button.success(
                    "applicant_close:" + applicantId,
                    "Close my ticket"),
                Button.secondary(
                    "applicant_keep:" + applicantId,
                    "Keep ticket open")
            ).queue();
        }

        case "moreinfo" -> event.reply(
            "Please ask <@" + applicantId
                + "> for the additional information needed in this ticket.")
            .setEphemeral(true).queue();

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

    if (event.getGuild() == null
            || !(event.getChannel() instanceof TextChannel channel)
            || !TICKET_CATEGORY_ID.equals(channel.getParentCategoryId())) {
        event.reply("This isn't a recognized application ticket.")
            .setEphemeral(true).queue();
        return;
    }

    String topic = channel.getTopic();
    if (topic == null || !topic.contains("applicantId=" + applicantId)) {
        event.reply("I couldn't verify the ticket owner.")
            .setEphemeral(true).queue();
        return;
    }

   event.deferReply(true).queue();

closeTicket(
    event.getGuild(),
    channel,
    applicantId,
    "Closed by applicant",
    message -> event.getHook().editOriginal(message).queue()
);
}



private void handleApplicantKeep(ButtonInteractionEvent event) {
    String applicantId = event.getComponentId()
        .substring("applicant_keep:".length());

    if (!event.getUser().getId().equals(applicantId)) {
        event.reply("Only the applicant who opened this ticket can choose this.")
            .setEphemeral(true).queue();
        return;
    }

    if (event.getGuild() == null
            || !(event.getChannel() instanceof TextChannel channel)
            || !TICKET_CATEGORY_ID.equals(channel.getParentCategoryId())) {
        event.reply("This isn't a recognized application ticket.")
            .setEphemeral(true).queue();
        return;
    }

    String topic = channel.getTopic();
    if (topic == null || !topic.contains("applicantId=" + applicantId)) {
        event.reply("I couldn't verify the ticket owner.")
            .setEphemeral(true).queue();
        return;
    }

    if (isClosed(channel)) {
        event.reply("This ticket is already closed. Ask staff to reopen it.")
            .setEphemeral(true).queue();
        return;
    }

    event.reply("No problem — the ticket will stay open.")
        .setEphemeral(true).queue();
}

public void forceClose(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null || event.getMember() == null) {
        event.reply("This command can only be used in the server.")
            .setEphemeral(true).queue();
        return;
    }

    Role staffRole = event.getGuild().getRoleById(STAFF_ROLE_ID);

    if (staffRole == null
            || !event.getMember().getRoles().contains(staffRole)) {
        event.reply("Only authorized staff can force-close tickets.")
            .setEphemeral(true).queue();
        return;
    }

    var selectedChannel = event.getOption("ticket").getAsChannel();
    String reason = event.getOption("reason").getAsString().trim();

    if (!(selectedChannel instanceof TextChannel channel)
            || !TICKET_CATEGORY_ID.equals(channel.getParentCategoryId())) {
        event.reply("Select a text channel in the application ticket category.")
            .setEphemeral(true).queue();
        return;
    }

    if (reason.isBlank()) {
        event.reply("A reason is required.")
            .setEphemeral(true).queue();
        return;
    }

    
String applicantId = extractApplicantId(channel);

if (applicantId.isBlank()) {
    event.reply("Couldn't identify the applicant from this ticket's topic.")
        .setEphemeral(true).queue();
    return;
}

event.deferReply(true).queue();

closeTicket(
    event.getGuild(),
    channel,
    applicantId,
    "Force-closed by " + event.getUser().getName()
        + ": " + reason,
    message -> event.getHook().editOriginal(message).queue()
);

}


private void closeTicket(
        net.dv8tion.jda.api.entities.Guild guild,
        TextChannel channel,
        String applicantId,
        String reason,
        java.util.function.Consumer<String> respond) {

    if (!TICKET_CATEGORY_ID.equals(channel.getParentCategoryId())) {
        respond.accept("This isn't an application ticket.");
        return;
    }

    String topic = channel.getTopic();
    if (topic == null
            || !topic.contains("applicantId=" + applicantId)) {
        respond.accept("Couldn't verify the ticket owner.");
        return;
    }

    Member applicant = guild.getMemberById(applicantId);
    if (applicant == null) {
        respond.accept("The applicant isn't currently in the server.");
        return;
    }

    channel.getManager()
        .putPermissionOverride(
            applicant,
            null,
            EnumSet.of(Permission.VIEW_CHANNEL)
        )
        .queue(
            success -> {
                channel.sendMessage(
                    "🔒 Ticket closed. **Reason:** " + reason
                ).queue();
                respond.accept("Ticket closed successfully.");
            },
            error -> respond.accept(
                "Couldn't close the ticket. Check the bot's permissions."
            )
        );
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

    String applicantId = topic.substring(start + marker.length()).trim();

    int separator = applicantId.indexOf('|');
    if (separator >= 0) {
        applicantId = applicantId.substring(0, separator).trim();
    }

    return applicantId;
}

public void reopenTicket(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null || event.getMember() == null) {
        event.reply("This command can only be used in the server.")
            .setEphemeral(true).queue();
        return;
    }

    Role staffRole = event.getGuild().getRoleById(STAFF_ROLE_ID);

    if (staffRole == null
            || !event.getMember().getRoles().contains(staffRole)) {
        event.reply("Only authorized staff can reopen tickets.")
            .setEphemeral(true).queue();
        return;
    }

    var selectedChannel = event.getOption("ticket").getAsChannel();

    if (!(selectedChannel instanceof TextChannel channel)
            || !TICKET_CATEGORY_ID.equals(channel.getParentCategoryId())) {
        event.reply("Select a text channel in the application ticket category.")
            .setEphemeral(true).queue();
        return;
    }

    String applicantId = extractApplicantId(channel);

    if (applicantId.isBlank()) {
        event.reply("Couldn't identify the applicant from the ticket topic.")
            .setEphemeral(true).queue();
        return;
    }

    Member applicant = event.getGuild().getMemberById(applicantId);

    if (applicant == null) {
        event.reply("The applicant isn't currently in the server.")
            .setEphemeral(true).queue();
        return;
    }

if (!isClosed(channel)) {
    event.reply("This ticket is already open.")
        .setEphemeral(true).queue();
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
                channel.sendMessage(
                    "🔓 This ticket has been reopened by staff."
                ).queue();

                event.getHook().editOriginal(
                    "Reopened " + channel.getAsMention() + "."
                ).queue();
            },
            error -> event.getHook().editOriginal(
                "Couldn't reopen the ticket. Check the bot's permissions."
            ).queue()
        );
}

private boolean isClosed(TextChannel channel) {
    String topic = channel.getTopic();
    return topic != null && topic.contains("status=closed");
}

private String updateTopicStatus(TextChannel channel, String status) {
    String topic = channel.getTopic();

    if (topic == null) {
        return "Mazebound application | status=" + status;
    }

    String updated = topic.replaceAll("\\s*\\|\\s*status=(open|closed)", "");
    return updated + " | status=" + status;
}

}

