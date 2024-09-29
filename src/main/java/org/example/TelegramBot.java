package org.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBot extends TelegramLongPollingBot {

    private static final String IDLE_STATE = "IDLE";
    private static final String AWAITS_CATEGORY_STATE = "AWAITS_CATEGORY";
    private static final String AWAITS_EXPENSE_STATE = "AWAITS_EXPENSE";

    private static final String ADD_EXPENSE_BUTTON = "Add Expense";
    private static final String SHOW_CATEGORIES_BUTTON = "Show Categories";
    private static final String SHOW_EXPENSES_BUTTON = "Show Expenses";
    private static final String SHOW_EXPENSES_DETAILED_BUTTON = "Show Expenses (detailed)";
    private static final String CANCEL_BUTTON = "Cancel";

    static List<String> INITIAL_KEYBOARD = List.of(
            ADD_EXPENSE_BUTTON,
            SHOW_CATEGORIES_BUTTON,
            SHOW_EXPENSES_BUTTON,
            SHOW_EXPENSES_DETAILED_BUTTON
    );

    private static final Map<Long, ChatState> CHATS_STATES = new HashMap<>();

    void changeState(
            String state,
            ChatState currentChatState,
            String stateData,
            long chatId,
            String text,
            Collection<String> buttons
    ) {
        System.out.println("State changed to: " + state);

        currentChatState.state = state;
        currentChatState.data = stateData;

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        List<String> buttonsWithCancel = new ArrayList<>(buttons);
        if (!state.equals(IDLE_STATE)) {
            buttonsWithCancel.add(CANCEL_BUTTON);
        }
        sendMessage.setReplyMarkup(buildKeyboard(buttonsWithCancel));
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            System.err.println("Failed to send a message");
            System.err.println(e);
        }
    }

    private static ReplyKeyboard buildKeyboard(List<String> btns) {
        if (btns == null || btns.isEmpty()) return new ReplyKeyboardRemove(true);
        var rows = new ArrayList<KeyboardRow>();
        for (var btn : btns) {
            KeyboardRow row = new KeyboardRow();
            row.add(btn);
            rows.add(row);
        }
        var markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    void handleIdle(Update update, ChatState state) {
        Message incommingMessage = update.getMessage();
        String messageText = incommingMessage.getText();
        Long chatId = incommingMessage.getChatId();

        switch (messageText) {
            case "/start" -> changeState(
                    IDLE_STATE,
                    state,
                    null,
                    chatId,
                    "Hi there!",
                    INITIAL_KEYBOARD
            );
            case ADD_EXPENSE_BUTTON -> changeState(
                    AWAITS_CATEGORY_STATE,
                    state,
                    null,
                    chatId,
                    "Choose or type new category",
                    state.expensesPerCategory.keySet()
            );
            case SHOW_CATEGORIES_BUTTON -> changeState(
                    IDLE_STATE,
                    state,
                    null,
                    chatId,
                    state.getCategories(),
                    INITIAL_KEYBOARD
            );
            case SHOW_EXPENSES_BUTTON -> changeState(
                    IDLE_STATE,
                    state,
                    null,
                    chatId,
                    state.getSpentPerCategories(),
                    INITIAL_KEYBOARD
            );
            case SHOW_EXPENSES_DETAILED_BUTTON -> changeState(
                    IDLE_STATE,
                    state,
                    null,
                    chatId,
                    state.getExpensesForEachCategory(),
                    INITIAL_KEYBOARD
            );
        }
    }

    void handleCategorySelection(Update update, ChatState state) {
        Message incommingMessage = update.getMessage();
        String messageText = incommingMessage.getText();
        state.expensesPerCategory.putIfAbsent(messageText, new ArrayList<>());
        changeState(
                AWAITS_EXPENSE_STATE,
                state,
                messageText,
                incommingMessage.getChatId(),
                "Enter your spent",
                Collections.emptyList()
        );
    }

    void handleExpenseEntering(Update update, ChatState state) {
        Message incommingMessage = update.getMessage();
        String messageText = incommingMessage.getText();
        Long chatId = incommingMessage.getChatId();
        state.expensesPerCategory.compute(state.data, (category, expenses) -> {
            var ex = expenses == null ? new ArrayList<Integer>() : expenses;
            ex.add(Integer.parseInt(messageText));
            return ex;
        });
        changeState(
                IDLE_STATE,
                state,
                null,
                chatId,
                "Accepted",
                INITIAL_KEYBOARD
        );
    }

    void handleUnknown(Update update, ChatState state) {
        changeState(
                IDLE_STATE,
                state,
                null,
                update.getMessage().getChatId(),
                "I don't know what to do",
                INITIAL_KEYBOARD
        );
        System.out.println("I don't know what to do");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Message message = update.getMessage();
        Long chatId = message.getChatId();
        CHATS_STATES.putIfAbsent(chatId, new ChatState());
        ChatState currentChatState = CHATS_STATES.get(chatId);

        String text = message.getText();
        System.out.println(text);
        if (text.equals(CANCEL_BUTTON)) {
            changeState(
                    IDLE_STATE,
                    currentChatState,
                    null,
                    chatId,
                    "Cancelled",
                    INITIAL_KEYBOARD
            );
            return;
        }

        System.out.println(currentChatState.state);
        switch (currentChatState.state) {
            case IDLE_STATE -> handleIdle(update, currentChatState);
            case AWAITS_CATEGORY_STATE -> handleCategorySelection(update, currentChatState);
            case AWAITS_EXPENSE_STATE -> handleExpenseEntering(update, currentChatState);
            default -> handleUnknown(update, currentChatState);
        }
    }

    @Override
    public String getBotUsername() {
        return "MyStatsBot";
    }

    @Override
    public String getBotToken() {
        return System.getenv("TG_BOT_TOKEN");
    }
}

class ChatState {
    String state = "IDLE";
    String data = null;
    Map<String, List<Integer>> expensesPerCategory = new HashMap<>();

    String getCategories() {
        if (expensesPerCategory.isEmpty()) return "Have no categories";
        return String.join(", ", expensesPerCategory.keySet());
    }

    String getSpentPerCategories() {
        if (expensesPerCategory.isEmpty()) return "Have no records";
        return expensesPerCategory.entrySet()
                .stream()
                .map(expense -> expense.getKey() + ": " + sum(expense.getValue()))
                .collect(Collectors.joining("\n"));
    }

    Integer sum(List<Integer> values) {
        return values.stream().reduce(0, Integer::sum);
    }

    String getExpensesForEachCategory() {
        if (expensesPerCategory.isEmpty()) return "No expenses to show";
        return expensesPerCategory.entrySet()
                .stream()
                .map(expense -> expense.getValue().stream().map(it -> expense.getKey() + ": " + it).collect(Collectors.joining("\n")))
                .collect(Collectors.joining("\n------------\n"));
    }
}