package Register;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;

import java.util.List;

public class Output {

    public static void answerListToChannel(List<Answer> answerList, IChannel channel, String title) {
        IGuild iGuild = channel.getGuild();
        if (answerList.size() == 0) {
            channel.sendMessage("Никого нет");
            return;
        }
        int position = 1;
        StringBuilder result = new StringBuilder();
        result.append(title).append("```cs\n");
        result.append(" # | Ник в дискорде          | Взвод       |кол | игровой ник\n");
        for (Answer answer : answerList) {
            result.append(String.format("%2d. ", position));
            String nick;
            if (iGuild.getUserByID(answer.discordID) == null)
                nick = "вышел-" + Long.toString(answer.discordID);
            else
                nick = iGuild.getUserByID(answer.discordID).getDisplayName(iGuild);
            result.append(String.format(" %-25s ", nick));
            result.append(answer.isSquad ? String.format("%-13s ", answer.squadName) : "  [-]         ");
            result.append(answer.numInSquad > 20 ? ">20  " : String.format("%-4d ", answer.numInSquad));
            result.append(answer.bfName);
            result.append("\n");
            position++;
        }
        result.append("```");

        channel.sendMessage(result.toString());
    }

    public static void eventListToChannel(List<event> events, IChannel channel, String title) {
        IGuild guild = channel.getGuild();
        if (events.size() == 0) {
            channel.sendMessage("Нет мероприятий");
            return;
        }
        StringBuilder result = new StringBuilder();
        boolean isResult = false;
        for (event ev : events) {
            if (guild.getChannelByID(ev.channelID) != null) {
                result.append(guild.getChannelByID(ev.channelID).mention());
                result.append("```").append(ev.description).append("```");
                isResult = true;
            }
        }
        if (isResult == false) {
            channel.sendMessage("Нет активных мероприятий");
        } else {
            channel.sendMessage(result.toString());
        }
    }
}
