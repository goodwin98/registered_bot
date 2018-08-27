package DiscordBot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.util.DiscordException;

public class main {
    private static final Logger log = LoggerFactory.getLogger(main.class);


    public static void main(String[] args) {
        IDiscordClient cli = BotUtils.getBuiltDiscordClient("NDgyODkwNjg0MTQ4MjE5OTA0.DmLe7Q.sYYOw3812jrrOyJfvxDteu4ZqLQ");

        cli.getDispatcher().registerListener(new DiscordEvents());
        try {

            cli.login();
        } catch (DiscordException e) {
            //System.err.println("Ошибка при подключении бота к Discord: " + e.getMessage());
            log.error("Error connect to discord", e);

            System.exit(1);
        }


    }
}
