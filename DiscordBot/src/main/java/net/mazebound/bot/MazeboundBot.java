package net.mazebound.bot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.entities.Message;

import java.awt.Color;

import java.util.concurrent.CompletableFuture;

public final class MazeboundBot extends ListenerAdapter {

    private final ApplicationTickets applicationTickets =
    new ApplicationTickets();

   
public static void main(String[] args) {
    String token = System.getenv("DISCORD_BOT_TOKEN");

    if (token == null || token.isBlank()) {
        throw new IllegalStateException(
            "Missing DISCORD_BOT_TOKEN environment variable."
        );
    }

    MinecraftServerStatus.startAutoRefresh();

    MazeboundBot bot = new MazeboundBot();

JDABuilder.createDefault(token)
    .addEventListeners(
        bot,
        bot.applicationTickets
    )
    .build();
}

  
@Override
public void onReady(ReadyEvent event) {
    event.getJDA().updateCommands()
        .addCommands(
            Commands.slash("help", "Show Mazebound bot commands"),
            Commands.slash("server", "Check Minecraft server status"),
            Commands.slash("rules", "View the Mazebound rules"),
            Commands.slash("postrules", "Post the permanent Mazebound rules")
                .setDefaultPermissions(
                    DefaultMemberPermissions.enabledFor(
                        Permission.ADMINISTRATOR
                    )
                ),
            Commands.slash(
                "postapplications",
                "Post the staff application panel"
            ).setDefaultPermissions(
                DefaultMemberPermissions.enabledFor(
                    Permission.ADMINISTRATOR
                )
            ),



            Commands.slash("forceclose", "Force-close an application ticket")
                .addOption(
                    OptionType.CHANNEL,
                    "ticket",
                    "The application ticket channel",
                    true
                )
                .addOption(
                    OptionType.STRING,
                    "reason",
                    "Why this ticket must be closed",
                    true
                )
                .setDefaultPermissions(
                    DefaultMemberPermissions.enabledFor(
                        Permission.MANAGE_CHANNEL
                    )
                ),
            Commands.slash("reopenticket", "Reopen a closed application ticket")
                .addOption(
                    OptionType.CHANNEL,
                    "ticket",
                    "The application ticket channel",
                    true
                )
                .setDefaultPermissions(
                    DefaultMemberPermissions.enabledFor(
                        Permission.MANAGE_CHANNEL
                    )
                )
        )
        .queue();


    System.out.println("Connected to Discord as "
        + event.getJDA().getSelfUser().getName());
}


    @Override
    public void onSlashCommandInteraction(
        SlashCommandInteractionEvent event
    ) {
        switch (event.getName()) {
            
case "help" -> event.reply(
    "**Mazebound Bot Commands**\n"
    + "`/help` — Show available commands\n"
    + "`/server` — Check Minecraft server status\n"
    + "`/rules` — View the Mazebound rules in #rules"
).setEphemeral(true).queue();

            case "server" -> checkServer(event);

            case "rules" -> event.reply(
    "📜 **Mazebound Rules**\n"
    + "View our rules in <#1539371067523600494>!"
).setEphemeral(true).queue();

case "postrules" -> postRules(event);

            default -> event.reply("Unknown command.")
                .setEphemeral(true).queue();

case "postapplications" ->
    applicationTickets.postPanel(event);
case "forceclose" ->
    applicationTickets.forceClose(event);
case "reopenticket" ->
    applicationTickets.reopenTicket(event);

        }
    }

    @Override
public void onMessageReceived(MessageReceivedEvent event) {
    // Ignore bots, including this bot.
    if (event.getAuthor().isBot()) {
        return;
    }

    String content = event.getMessage().getContentRaw().trim();

    if (!content.toLowerCase().startsWith("m!")) {
        return;
    }

    String command = content.substring(2).trim().toLowerCase();

    switch (command) {
        case "help" -> event.getChannel().sendMessage(
            "**Mazebound Bot Commands**\n"
            + "`m!help` — Show available commands\n"
            + "`m!server` — Check Minecraft server status\n"
            + "`m!rules` — View the Mazebound rules"
        ).queue();

        case "server" -> event.getChannel().sendMessage(
            MinecraftServerStatus.query().online()
                ? "**Mazebound Server: Online**"
                : "**Mazebound Server: Offline or unreachable**"
        ).queue();

        case "rules" -> event.getChannel().sendMessage(
            "📜 **Mazebound Rules**\n"
            + "View our rules in <#1539371067523600494>!"
        ).queue();

        default -> event.getChannel().sendMessage(
            "Unknown command. Use `m!help`."
        ).queue();
    }
}

    private void checkServer(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue(hook ->
            CompletableFuture.supplyAsync(MinecraftServerStatus::query)
                .thenAccept(status -> {
                    String message;

                    if (status.online()) {
                        message = "**Mazebound Server: Online**\n"
                            + "**Version:** " + status.version() + "\n"
                            + "**Players:** " + status.playersOnline()
                            + "/" + status.maxPlayers();
                    } else {
                        message = "**Mazebound Server: Offline or "
                            + "unreachable**\n"
                            + "The status check did not receive a valid "
                            + "response.";
                    }

                    hook.editOriginal(message).queue();
                })
                .exceptionally(error -> {
                    hook.editOriginal(
                        "Couldn't complete the server status check."
                    ).queue();
                    return null;
                })
        );
    }

    private void postRules(SlashCommandInteractionEvent event) {
    if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
        event.reply("You need administrator permissions to post the rules.")
            .setEphemeral(true)
            .queue();
        return;
    }

    TextChannel channel = event.getJDA()
        .getTextChannelById("1539371067523600494");

    if (channel == null) {
        event.reply("I couldn't find the rules channel.")
            .setEphemeral(true)
            .queue();
        return;
    }

    event.reply("Preparing to post the Mazebound rules in <#1539371067523600494>.")
        .setEphemeral(true)
        .queue();

    
    Color accent = new Color(245, 130, 32);

    EmbedBuilder introduction = new EmbedBuilder()
        .setColor(accent)
        .setTitle("📜 Mazebound Rules")
        .setDescription(
            "Welcome to Mazebound!\n\n"
            + "These rules keep our community fair, welcoming, "
            + "and fun for everyone. By participating in the "
            + "Mazebound Discord or Minecraft server, you agree "
            + "to follow them."
        );

    EmbedBuilder discordRules = new EmbedBuilder()
        .setColor(accent)
        .setTitle("💬 Discord Rules")
        .setDescription(
            "**1. Be respectful**\n"
            + "Treat other members with respect. No harassment, "
            + "bullying, discrimination, personal attacks, or "
            + "deliberately hostile behavior.\n\n"

            + "**2. No spam**\n"
            + "Don't flood channels with repeated messages, "
            + "excessive mentions, emojis, or other disruptive content.\n\n"

            + "**3. Keep conversations appropriate**\n"
            + "No NSFW, sexually explicit, excessively graphic, "
            + "or otherwise inappropriate content.\n\n"

            + "**4. Use the correct channels**\n"
            + "Keep conversations where they belong. Use the "
            + "appropriate channels for suggestions, bugs, "
            + "Minecraft chat, Citadel discussion, etc.\n\n"

            + "**5. No unsolicited advertising**\n"
            + "Don't advertise other servers, communities, products, "
            + "or services without permission from the Mazebound team.\n\n"

            + "**6. No malicious content**\n"
            + "No scams, phishing, malware, suspicious links, "
            + "account theft attempts, or attempts to compromise "
            + "another person's account.\n\n"

            + "**7. Don't impersonate others**\n"
            + "Don't impersonate staff, developers, bots, "
            + "or other community members.\n\n"

            + "**8. Don't abuse bots or Discord features**\n"
            + "No command spam, bot abuse, webhook abuse, "
            + "or attempts to disrupt the server.\n\n"

            + "**9. Respect spoilers**\n"
            + "Use spoiler tags and designated spoiler channels "
            + "when discussing major discoveries, bosses, Citadel "
            + "content, puzzles, or unreleased information.\n\n"

            + "**10. Follow staff instructions**\n"
            + "Staff decisions should be respected. If you disagree, "
            + "discuss it privately rather than creating drama in public."
        );

    EmbedBuilder minecraftRules = new EmbedBuilder()
        .setColor(accent)
        .setTitle("⛏️ Minecraft Rules")
        .setDescription(
            "**1. No cheating**\n"
            + "No hacked clients, unfair modifications, exploits, "
            + "macros, autoclickers, or other tools that provide "
            + "an unfair advantage.\n\n"

            + "**2. No exploiting bugs**\n"
            + "If you discover an exploit or serious bug, report it "
            + "instead of abusing it.\n\n"

            + "**3. No griefing**\n"
            + "Don't intentionally destroy, alter, or sabotage "
            + "another player's builds or creations.\n\n"

            + "**4. No stealing**\n"
            + "Don't take another player's items, resources, "
            + "or belongings without permission.\n\n"

            + "**5. Respect protected areas**\n"
            + "Don't attempt to bypass protections around the maze, "
            + "Citadel, monuments, bosses, or other special areas.\n\n"

            + "**6. No bypassing progression**\n"
            + "Don't use exploits, glitches, alternate accounts, "
            + "or other methods to bypass intended progression systems.\n\n"

            + "**7. No combat abuse**\n"
            + "Don't repeatedly target players who aren't participating "
            + "in PvP or use mechanics to intentionally ruin their experience.\n\n"

            + "**8. No lag machines**\n"
            + "Don't intentionally create machines, farms, redstone "
            + "contraptions, or other setups designed to cause excessive lag.\n\n"

            + "**9. Don't abuse unintended mechanics**\n"
            + "If something clearly isn't working as intended, report it. "
            + "Don't build your progression around it.\n\n"

            + "**10. Don't ruin discoveries for others**\n"
            + "Major discoveries—especially Citadel clues, puzzles, "
            + "locations, and unrevealed mechanics—shouldn't be "
            + "deliberately spoiled for players who haven't discovered them.\n\n"

            + "**11. Use common sense**\n"
            + "Not everything can be covered by a numbered rule. "
            + "Staff may take action against deliberate attempts to "
            + "ruin the experience, even if the exact behavior isn't listed."
        );

    EmbedBuilder enforcement = new EmbedBuilder()
        .setColor(accent)
        .setTitle("⚖️ Enforcement")
        .setDescription(
            "Depending on the situation, violations may result in:\n\n"
            + "⚠️ Warning → Temporary restriction → Temporary ban "
            + "→ Permanent ban\n\n"
            + "**Severe violations may skip earlier steps.**\n\n"
            + "Staff may act based on the intent and impact of someone's "
            + "behavior, not just whether they found a loophole.\n\n"
            + "The goal isn't to punish people. It's to protect the "
            + "experience we're building together."
        )
        .setFooter("Rules may be updated as Mazebound develops. Major changes may be announced.");

    channel.sendMessageEmbeds(
        introduction.build(),
        discordRules.build(),
        minecraftRules.build(),
        enforcement.build()
    ).queue(
        success -> System.out.println("Mazebound rules posted."),
        error -> System.err.println(
            "Failed to post rules: " + error.getMessage()
        )
    );
}
}
