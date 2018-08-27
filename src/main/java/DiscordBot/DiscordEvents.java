package DiscordBot;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;

import java.util.*;

public class DiscordEvents {
    private static Map<String, Command> commandMap = new HashMap<>();

    private static Runnable minutesTask;

    // Statically populate the commandMap with the intended functionality
    // Might be better practise to do this from an instantiated objects constructor
    static {

        commandMap.put("go", (event, args) -> BotUtils.newUserForRegister(event.getAuthor(), event.getGuild(), event.getChannel()));

        commandMap.put("new_event", (event, args) -> BotUtils.createEvent(args, event.getChannel(), event.getAuthor()));

        commandMap.put("list_all", (event, args) -> BotUtils.displayAllAnswers(args, event.getChannel(), event.getGuild(), event.getAuthor()));

        commandMap.put("delete_event", ((event, args) -> BotUtils.deleteEvent(event.getChannel(), args, event.getAuthor())));

        commandMap.put("delete_user", ((event, args) -> BotUtils.deleteUser(event.getChannel(), event.getAuthor(), args)));

        commandMap.put("list_events", ((event, args) -> BotUtils.displayAllEvents(event.getChannel(), event.getAuthor())));

        minutesTask = () -> {
            BotUtils.deleteTerminatedForms();
        };
        BotUtils.set5MinutesTimer(minutesTask);


    }


    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent event) {

        if (event.getGuild() == null) {
            BotUtils.sendToFormByUser(event.getAuthor(), event.getMessage().getContent());
        } else {

            String[] argArray = event.getMessage().getContent().split(" ");

            // First ensure at least the command and prefix is present, the arg length can be handled by your command func
            if (argArray.length == 0)
                return;

            // Check if the first arg (the command) starts with the prefix defined in the utils class
            if (!argArray[0].startsWith(BotUtils.BOT_PREFIX))
                return;

            // Extract the "command" part of the first arg out by just ditching the first character
            String commandStr = argArray[0].substring(1);

            // Load the rest of the args in the array into a List for safer access
            List<String> argsList = new ArrayList<>(Arrays.asList(argArray));
            argsList.remove(0); // Remove the command

            // Instead of delegating the work to a switch, automatically do it via calling the mapping if it exists

            if (commandMap.containsKey(commandStr))
                commandMap.get(commandStr).runCommand(event, argsList);
        }
    }

    @EventSubscriber
    public void onAddReaction(ReactionAddEvent event) {
        if (event.getGuild() == null) {
            BotUtils.sendReaction(event.getUser(), event.getReaction(), event.getMessage());
        }
    }
}
