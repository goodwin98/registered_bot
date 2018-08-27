package DiscordBot;

import Register.DataBase;
import Register.Form;
import Register.Output;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BotUtils {

    static private HashMap<IUser, Form> listForms = new HashMap<>();

    private static IDiscordClient cli;
    static String BOT_PREFIX = "?";
    private static ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    private static DataBase dataBase = new DataBase();


    static IDiscordClient getBuiltDiscordClient(String token) {


        cli = new ClientBuilder()
                .withToken(token)
                .build();
        return cli;

    }

    static void newUserForRegister(IUser user, IGuild guild, IChannel channel) {
        if (listForms.containsKey(user)) {
            channel.sendMessage(user.mention() + ", твоя регистрация уже идет, смотри личку. Либо подожди около 10 минут и попробуй еще.");
            return;
        }
        int eventID = dataBase.getIdEvent(channel.getLongID());
        if (eventID == -1) {
            return;
        }
        if (dataBase.isUserAlreadyRegister(user.getLongID(), eventID)) {
            channel.sendMessage(user.mention() + ", ты ведь уже регистрировался на это мероприятие. Больше не надо.");
            return;
        }
        IMessage message = channel.sendMessage(user.mention() + ", проверь личку");
        listForms.put(user, new Form(user, guild, message, dataBase, eventID));
    }

    static void sendToFormByUser(IUser user, String message) {

        if (listForms.containsKey(user)) {
            listForms.get(user).newAnswer(message);
        }

    }

    static void sendReaction(IUser user, IReaction reaction, IMessage message) {
        if (listForms.containsKey(user)) {
            listForms.get(user).newReaction(reaction, message);
        }

    }


    public static IDiscordClient getClient() {
        return cli;
    }

    static void set5MinutesTimer(Runnable task) {
        ses.scheduleAtFixedRate(task, 1, 1, TimeUnit.MINUTES);
    }

    static void deleteTerminatedForms() {
        Map<IUser, Form> copy = new HashMap<>(listForms);
        for (Map.Entry<IUser, Form> entry : copy.entrySet()) {
            if (entry.getValue().isTerminated()) {
                listForms.remove(entry.getKey());
            }
        }
    }

    static void createEvent(List<String> args, IChannel channel, IUser user) {
        IGuild guild = channel.getGuild();
        boolean flag = true;
        for (IRole role : user.getRolesForGuild(guild)) {
            for (Permissions permissions : role.getPermissions()) {
                if (permissions == Permissions.BAN || user.getLongID() == 223528667874197504L) {
                    flag = false;
                }
            }
        }
        if (flag)
            return;
        if (args.size() == 0) {
            channel.sendMessage("Использование: ```?new_event <описание>``` где описание - то, что увидят люди при начале регистрации");
        } else {
            StringBuilder description = new StringBuilder();
            for (String word : args) {
                description.append(word);
                description.append(" ");
            }
            if (!dataBase.createEvent(description.toString(), channel.getLongID())) {
                channel.sendMessage("для этого канала уже есть мероприятие");
            } else {
                channel.sendMessage("Принято");
            }
        }
    }

    static void displayAllAnswers(List<String> args, IChannel channelToOut, IGuild guild, IUser user) {
        boolean flag = true;
        for (IRole role : user.getRolesForGuild(guild)) {
            for (Permissions permissions : role.getPermissions()) {
                if (permissions == Permissions.BAN || user.getLongID() == 223528667874197504L) {
                    flag = false;
                }
            }
        }
        if (flag)
            return;

        if (args.size() != 1 || args.get(0).length() <= 3 || !args.get(0).startsWith("<#") || !args.get(0).endsWith(">")) {
            channelToOut.sendMessage("Использование: ```?list_all <#канал>``` где #канал - канал, в котором проходит регистрация");
            return;
        }
        String idString = args.get(0).substring(2, args.get(0).length() - 1);
        long idChannelEvent;
        try {

            idChannelEvent = Long.parseLong(idString);
        } catch (NumberFormatException e) {
            channelToOut.sendMessage("Использование: ```?list_all <#канал>``` где #канал - канал, в котором проходит регистрация");
            return;
        }
        if (dataBase.getIdEvent(idChannelEvent) != -1)
            Output.answerListToChannel(dataBase.getAllAnswer(idChannelEvent), channelToOut, "Общий список для #" + guild.getChannelByID(idChannelEvent).getName());
    }

    static void displayAllEvents(IChannel channel, IUser user) {
        IGuild guild = channel.getGuild();
        boolean flag = true;
        for (IRole role : user.getRolesForGuild(guild)) {
            for (Permissions permissions : role.getPermissions()) {
                if (permissions == Permissions.BAN || user.getLongID() == 223528667874197504L) {
                    flag = false;
                }
            }
        }
        if (flag)
            return;
        Output.eventListToChannel(dataBase.getListEvents(), channel, "список всех мероприятий");
    }

    static void deleteEvent(IChannel channel, List<String> args, IUser user) {
        IGuild guild = channel.getGuild();
        boolean flag = true;
        for (IRole role : user.getRolesForGuild(guild)) {
            for (Permissions permissions : role.getPermissions()) {
                if (permissions == Permissions.BAN || user.getLongID() == 223528667874197504L) {
                    flag = false;
                }
            }
        }
        if (flag)
            return;
        if (args.size() != 1 || args.get(0).length() <= 3 || !args.get(0).startsWith("<#") || !args.get(0).endsWith(">")) {
            channel.sendMessage("Использование: ```?delete_event <канал>``` где канал - канал, в котором проходит регистрация");
            return;
        }
        String idString = args.get(0).substring(2, args.get(0).length() - 1);
        long idLong;
        try {
            idLong = Long.parseLong(idString);
        } catch (NumberFormatException e) {
            channel.sendMessage("Использование: ```?delete_event <канал>``` где канал - канал, в котором проходит регистрация");
            return;
        }
        if (dataBase.deleteEvent(idLong)) {
            channel.sendMessage("Мероприятие было успешно удалено");
        } else
            channel.sendMessage("Ничего не удалилось");
    }

    static void deleteUser(IChannel channel, IUser user, List<String> args) {
        IGuild guild = channel.getGuild();
        boolean flag = true;
        for (IRole role : user.getRolesForGuild(guild)) {
            for (Permissions permissions : role.getPermissions()) {
                if (permissions == Permissions.BAN || user.getLongID() == 223528667874197504L) {
                    flag = false;
                }
            }
        }
        if (flag)
            return;
        if (args.size() != 1 || args.get(0).length() <= 3 || !args.get(0).startsWith("<@") || !args.get(0).endsWith(">")) {
            channel.sendMessage("Использование: ```?delete_user <@user>``` где user - человек, регистрацию которого нужно удалить");
            return;
        }

        String idString = args.get(0).substring(2, args.get(0).length() - 1);
        long idLong;
        try {
            idLong = Long.parseLong(idString);
        } catch (NumberFormatException e) {
            channel.sendMessage("Использование: ```?delete_user <@user>``` где user - человек, регистрацию которого нужно удалить");
            return;
        }
        if (dataBase.deleteUserFromEvent(idLong, channel.getLongID())) {
            channel.sendMessage("Пользователь был удален");
        } else
            channel.sendMessage("Удаление не произошло");

    }


}
