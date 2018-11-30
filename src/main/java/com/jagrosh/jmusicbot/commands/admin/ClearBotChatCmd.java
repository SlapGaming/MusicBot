package com.jagrosh.jmusicbot.commands.admin;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.Config;
import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.core.entities.Message;

import java.util.List;
import java.util.stream.Collectors;

public class ClearBotChatCmd extends Command {

    private Config config;
    private Bot bot;

    public ClearBotChatCmd(Bot bot, Config config) {
        this.name = "clear";
        this.aliases = new String[]{"c"};
        this.help = "clear the commands and replies in a text channel by checking the previous <?messages=25> messages.";
        this.arguments = "<?messages=25>";
        this.guildOnly = true;
        this.category = bot.ADMIN;
        this.bot = bot;
        this.config = config;
    }

    @Override
    protected void execute(CommandEvent event) {
        //delete the message that triggered the clean
        event.getMessage().delete().queue();

        //parse arguments
        int limit = 25;
        if (!event.getArgs().equals("")) {
            String[] args = event.getArgs().split("\\s+");
            try {
                limit = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                event.replyInDm("First argument should be a number. [10-100]");
                return;
            }

            if (limit < 10 || limit > 100) {
                event.replyInDm("First argument should be a number. [10-100]");
                return;
            }
        }

        //Notify user in DM search has started.
        event.replyInDm(EmojiParser.parseToUnicode("Trying to find commands/replies in " + event.getChannel().getName()
                + ", limited to last " + limit + " messages. :mag_right: "));

        //Fetch last <?messages=25> and start delete process.
        event.getChannel().getHistoryBefore(event.getMessage(), limit).queue(
                (messageHistory -> {
                    if (messageHistory.isEmpty()) {
                        event.replyInDm(EmojiParser.parseToUnicode("Could not find any commands/replies to delete. :shrug:"));
                    } else {
                        List<Message> targetMessages = messageHistory.getRetrievedHistory().stream()
                                .filter(m -> m.getAuthor().equals(bot.getJDA().getSelfUser())
                                        || m.getContentRaw().startsWith(config.getPrefix())
                                        || m.getContentRaw().startsWith(config.getAltPrefix()))
                                .collect(Collectors.toList());

                        if (targetMessages.size() > 0) {
                            event.replyInDm(EmojiParser.parseToUnicode("Queued " + targetMessages.size() + " for deletion :wastebasket:"));
                            targetMessages.forEach(message -> message.delete().queue(null, t -> event.replyInDm("Failed to delete a message...")));
                        } else {
                            event.replyInDm(EmojiParser.parseToUnicode("Could not find any commands/replies to delete. :shrug:"));

                        }
                    }
                })
        );
    }
}
