package ru.artak.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.artak.client.strava.StravaCredential;
import ru.artak.client.telegram.TelegramClient;
import ru.artak.client.telegram.model.GetUpdateTelegram;
import ru.artak.storage.Storage;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TelegramService {

    private static final Logger logger = LogManager.getLogger(TelegramService.class);

    private final Object lock = new Object();

    private final TelegramClient telegramClient;

    private final StravaService stravaService;

    private final Storage storage;

    private final String telegramBotDefaultText =
            "Телеграм бот для работы с ресурсом Strava.com\n" +
                    "\n" +
                    "Список доступных команд:\n" +
                    "\n" +
                    "/auth - авторизация через OAuth\n" +
                    "/weekdistance - набеганное расстояние за календарную неделю\n" +
                    "/deauthorize - деавторизация\n ";

    private final String whenUserAlreadyAuthorized = "Вы уже авторизованы!";
    private final String telegramNoAuthorizationText = "Вы не авторизованы. Используйте команду /auth";
    private final String telegramDeauthorizeText = "Вы деавторизированы!";

    private final String telegramWeekDistanceText = "Вы пробежали ";
    private final String errorText = "Ошибка, пожалуйста повторите позднее ";


    public TelegramService(TelegramClient telegramClient, Storage storage, StravaService stravaService) {
        this.telegramClient = telegramClient;
        this.storage = storage;
        this.stravaService = stravaService;
    }

    public void sendGet() {
        Integer telegramOffset = 0;

        while (true) {
            synchronized (lock) {

                try {
                    GetUpdateTelegram getUpdateTelegram = telegramClient.getUpdates(telegramOffset);

                    List<TelegramUserInfo> updateIds = getAllTelegramUpdateUsers(getUpdateTelegram);

                    for (TelegramUserInfo id : updateIds) {
                        Integer lastUpdateId = getUpdateTelegram.getResult().get(getUpdateTelegram.getResult().size() - 1).getUpdateId();
                        Integer updateId = id.getUpdateId();
                        Long chatId = id.getChatId();
                        String text = id.getText();

                        if (updateId <= lastUpdateId) {
                            logger.debug("received a new request in telegram from user - {}", chatId);
                            switch (text) {
                                case "/auth":
                                    handleAuthCommand(chatId);
                                    break;
                                case "/weekdistance":
                                    handleWeekDistance(chatId);
                                    break;
                                case "/deauthorize":
                                    handleDeauthorizeCommand(chatId);
                                    break;
                                default:
                                    handleDefaultCommand(chatId, telegramBotDefaultText);
                                    break;
                            }
                        }
                        telegramOffset = lastUpdateId;
                    }
                    lock.wait(500);
                } catch (Throwable e) {
                    logger.error("mistake telegram bot handler ", e);
                }
            }
        }

    }

    private void handleWeekDistance(Long chatId) throws IOException, InterruptedException {
        logger.debug("received a request /weekdistance for user - {}", chatId);
        StravaCredential credential = storage.getStravaCredentials(chatId);

        if (credential == null || credential.isStatus()) {
            handleDefaultCommand(chatId, telegramNoAuthorizationText);
            return;
        }
        Number weekDistance = stravaService.getRunningWeekDistance(chatId, credential);
        telegramClient.sendDistanceText(chatId, telegramWeekDistanceText, weekDistance);

    }

    private void handleDefaultCommand(Long chatId, String anyText) throws IOException, InterruptedException {
        telegramClient.sendSimpleText(chatId, anyText);
    }

    private void handleAuthCommand(Long chatId) throws IOException, InterruptedException {
        final UUID randomClientID = UUID.randomUUID();
        logger.debug("received a request /auth for user - {}, randomClientID - {}", chatId, randomClientID);
        StravaCredential credential = storage.getStravaCredentials(chatId);

        if (credential != null && !credential.isStatus()) {
            handleDefaultCommand(chatId, whenUserAlreadyAuthorized);

            return;
        }
        storage.saveStateForUser(randomClientID, chatId);
        telegramClient.sendOauthCommand(randomClientID, chatId);
    }

    private void handleDeauthorizeCommand(Long chatId) throws IOException, InterruptedException {
        logger.debug("received a request /deauthorize for user - {}", chatId);
        StravaCredential credential = storage.getStravaCredentials(chatId);

        if (credential == null) {
            telegramClient.sendSimpleText(chatId, telegramNoAuthorizationText);
            return;
        }
        String accessToken = credential.getAccessToken();
        try {
            stravaService.deauthorize(chatId, accessToken);
            telegramClient.sendSimpleText(chatId, telegramDeauthorizeText);
        } catch (Exception e) {
            telegramClient.sendSimpleText(chatId, errorText);
            logger.warn("failed to deauthorize user - {}", chatId, e);
        }

    }

    private List<TelegramUserInfo> getAllTelegramUpdateUsers(GetUpdateTelegram getUpdateTelegram) {
        return getUpdateTelegram.getResult().stream()
                .map(result ->
                        new TelegramUserInfo(
                                result.getMessage().getChat().getId(),
                                result.getMessage().getText(),
                                result.getUpdateId()))
                .collect(Collectors.toList());
    }

}