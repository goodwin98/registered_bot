package Register;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Form {
    private static final Logger log = LoggerFactory.getLogger(Form.class);

    private int numberOfQuestion;
    private IUser discordIUser;
    private IGuild discordGuild;
    private boolean isTerminated;
    private Answer answer;
    private IMessage lastMessage;
    private long lastActivityTime;
    private int eventID;
    private IMessage welcomeMessage;
    private final long ACTIVITY_TIME_OUT = 600000l; // 10 минут
    ReactionEmoji negativeEmoji;
    ReactionEmoji positiveEmoji;
    private DataBase dataBase;

    private final List<Quest> questList = Collections.unmodifiableList(
            new ArrayList<Quest>() {{
                add(new Quest("Хотите приступить к регистрации?", (message -> continueOrNot(message)), true, true));
                add(new Quest("Напишите ваш игровой ник", (message -> addGameNick(message)), false, false));
                add(new Quest("Вы состоите во взводе? Если да, то напишите название.", (message -> addGameSquad(message)), false, true));
                add(new Quest("Вы представляете свой взвод(вы командир и будете выступать взводом)?", (message -> addIsSquaded(message)), true, true));
                add(new Quest("Сколько человек будет в вашем взводе включая вас?(максимальное количество игроков - 5)", (message -> numMenInSquad(message)), false, false));
                add(new Quest("Все ли верно введено?", (message -> correctOrNot(message)), false, false));

            }}
    );


    public Form(IUser discordUser, IGuild guild, IMessage welcomeMessage, DataBase dataBase, int eventID) {
        this.dataBase = dataBase;
        this.eventID = eventID;
        discordIUser = discordUser;
        discordGuild = guild;
        this.welcomeMessage = welcomeMessage;
        negativeEmoji = ReactionEmoji.of("WrongMark", 479226160811737099l);
        positiveEmoji = ReactionEmoji.of("CheckMark", 479226160493101056l);
        init();

    }

    void init() {
        numberOfQuestion = 0;
        IPrivateChannel channel = discordIUser.getOrCreatePMChannel();
        channel.sendMessage(dataBase.getDescription(eventID));

        lastMessage = channel.sendMessage(questList.get(numberOfQuestion).getText());
        addReaction(lastMessage, questList.get(numberOfQuestion));
        lastActivityTime = System.currentTimeMillis();
        answer = new Answer();
        answer.discordID = discordIUser.getLongID();
        answer.discordName = discordIUser.getDisplayName(discordGuild);
    }

    public void newAnswer(String message) {
        if (isTerminated) {
            return;
        }
        questList.get(numberOfQuestion).getHandle().runCommand(message);

        lastActivityTime = System.currentTimeMillis();

        if (isTerminated) {
            return;
        }
        if (numberOfQuestion + 1 >= questList.size()) {
            viewAllInfo();
        }
    }

    public void newReaction(IReaction reaction, IMessage iMessage) {
        if (isTerminated || !lastMessage.equals(iMessage)) {
            return;
        }
        if (reaction.getEmoji().equals(positiveEmoji))
            questList.get(numberOfQuestion).getHandle().runCommand("+");
        else if (reaction.getEmoji().equals(negativeEmoji))
            questList.get(numberOfQuestion).getHandle().runCommand("-");

        lastActivityTime = System.currentTimeMillis();

        if (isTerminated) {
            return;
        }

        if (numberOfQuestion + 1 >= questList.size()) {
            viewAllInfo();
        }
    }

    private void addToOther(String message) {
        numberOfQuestion += 1;
    }

    private void correctOrNot(String message) {
        if (message.equals("-")) {
            discordIUser.getOrCreatePMChannel().sendMessage("Хорошо, давайте еще раз");
            init();
        } else if (message.equals("+")) {
            isTerminated = true;
            log.info("mark form user " + discordIUser.getName() + " isTerminated");
            if (dataBase.saveForm(answer, eventID)) {
                discordIUser.getOrCreatePMChannel().sendMessage("Спасибо за регистрацию, до свидания");
                welcomeMessage.edit(discordIUser.mention() + " спасибо за регистрацию.");
            } else {
                discordIUser.getOrCreatePMChannel().sendMessage("что-то пошло не так во время сохранения формы. Повторите, пожалуйста позже");
                welcomeMessage.edit(discordIUser.mention() + " что-то пошло не так во время сохранения формы. Повторите, пожалуйста позже.");
            }
        }
    }

    private void numMenInSquad(String message) {
        int num;
        try {

            num = Integer.parseInt(message);
        } catch (NumberFormatException e) {
            num = 0;
        }
        if (num > 0) {
            answer.numInSquad = num;
            numberOfQuestion++;

            lastMessage = sendQuestToUser(discordIUser, questList.get(numberOfQuestion).getText());
        } else {
            discordIUser.getOrCreatePMChannel().sendMessage("Нужно написать число");
        }
    }

    private void continueOrNot(String message) {
        if (message.equals("-")) {
            isTerminated = true;
            discordIUser.getOrCreatePMChannel().sendMessage("Хорошо, до свидания");
        } else if (message.equals("+")) {
            numberOfQuestion += 1;
            lastMessage = sendQuestToUser(discordIUser, questList.get(numberOfQuestion).getText());
            addReaction(lastMessage, questList.get(numberOfQuestion));
        }
    }

    private void addGameNick(String message) {
        if (message.length() > 30) {
            discordIUser.getOrCreatePMChannel().sendMessage("Слишком длинный ник");
            return;
        }
        answer.bfName = trim(message);
        numberOfQuestion += 1;

        lastMessage = sendQuestToUser(discordIUser, questList.get(numberOfQuestion).getText());
        addReaction(lastMessage, questList.get(numberOfQuestion));
    }

    private void addGameSquad(String message) {
        if (message.length() > 30) {
            discordIUser.getOrCreatePMChannel().sendMessage("Слишком длинное название");
            return;
        }
        if (message.equals("-")) {
            answer.squadName = "";
            answer.numInSquad = 1;
            numberOfQuestion += 3;
        } else if (!message.equals("+")) {
            answer.squadName = trim(message);
            numberOfQuestion++;
        }
        lastMessage = sendQuestToUser(discordIUser, questList.get(numberOfQuestion).getText());
        addReaction(lastMessage, questList.get(numberOfQuestion));
    }

    private void addIsSquaded(String message) {
        if (message.equals("+")) {
            answer.isSquad = true;
            numberOfQuestion++;
        } else if (message.equals("-")) {
            answer.isSquad = false;
            answer.numInSquad = 1;
            numberOfQuestion += 2;
        }

        lastMessage = sendQuestToUser(discordIUser, questList.get(numberOfQuestion).getText());
        addReaction(lastMessage, questList.get(numberOfQuestion));
    }

    public boolean isTerminated() {
        if ((System.currentTimeMillis() - lastActivityTime) > ACTIVITY_TIME_OUT)
            log.info("User " + discordIUser.getName() + " timeout");

        return isTerminated || (System.currentTimeMillis() - lastActivityTime) > ACTIVITY_TIME_OUT;
    }


    private void addReaction(IMessage message, Quest quest) {
        if (quest.isPositiveNeed())
            RequestBuffer.request(() -> message.addReaction(positiveEmoji));
        if (quest.isNegativeNeed())
            RequestBuffer.request(() -> message.addReaction(negativeEmoji));

    }

    private void viewAllInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Никнейм: ");
        info.append(answer.bfName);
        info.append("\n");
        info.append("Взвод: ");
        info.append(answer.squadName == "" ? "Без взвода" : answer.squadName);
        info.append("\n");
        info.append("Участие взводом?: ");
        info.append(answer.isSquad ? "да" : "нет");
        info.append("\n");
        info.append("Всего в вашей команде: ");
        info.append(answer.numInSquad == 0 ? 1 : answer.numInSquad);
        lastMessage = discordIUser.getOrCreatePMChannel().sendMessage(info.toString());

        RequestBuffer.request(() -> lastMessage.addReaction(positiveEmoji));
        RequestBuffer.request(() -> lastMessage.addReaction(negativeEmoji));
    }

    private IMessage sendQuestToUser(IUser user, String message) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.appendField("регистрация", message, true);
        return user.getOrCreatePMChannel().sendMessage(builder.build());
    }

    private String trim(String text) {
        String result = text.replace("`", "'");
        result = result.replace("\n", " ");

        return result;
    }
}
