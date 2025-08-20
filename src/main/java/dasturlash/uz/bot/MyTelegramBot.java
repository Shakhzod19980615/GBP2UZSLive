package dasturlash.uz.bot;

import dasturlash.uz.dto.RateDto;
import dasturlash.uz.scrapers.MoneffScraper;
import dasturlash.uz.scrapers.PaysendScraper;
import dasturlash.uz.scrapers.ProfeeScraper;
import dasturlash.uz.scrapers.TransferGoScraper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

@Component
public class MyTelegramBot extends TelegramLongPollingBot {
    @Value("${telegram.bot.username}") private String botUsername;
    @Value("${telegram.bot.token}") private String botToken;
    private final ProfeeScraper profeeScraper;
    private final MoneffScraper moneffScraper;
    private final TransferGoScraper transferGoScraper;
    private final PaysendScraper paysendScraper;
    //private final XeScraper xeScraper;

    public MyTelegramBot(ProfeeScraper profeeScraper, MoneffScraper moneffScraper,
                         TransferGoScraper transferGoScraper, PaysendScraper paysendScraper) {
        this.profeeScraper = profeeScraper;
        this.moneffScraper = moneffScraper;
        this.transferGoScraper = transferGoScraper;
        this.paysendScraper = paysendScraper;
    }

    private static class RateInfo {
        String name;
        BigDecimal rate;
        BigDecimal totalReceived;

        RateInfo(String name, BigDecimal rate, BigDecimal totalReceived) {
            this.name = name;
            this.rate = rate;
            this.totalReceived = totalReceived;
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String msg = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            try {
                BigDecimal amountGBP = new BigDecimal(msg.trim());

                Optional<RateDto> rateOptProfee = profeeScraper.scrape();
                Optional<RateDto> rateOptTransferGo = transferGoScraper.scrape();
                Optional<RateDto> rateOptMoneff = moneffScraper.scrape();
                Optional<RateDto> rateOptPaysend = paysendScraper.scrape();
                //Optional<RateDto> rateOptXe = xeScraper.scrape();

                if (rateOptProfee.isEmpty() || rateOptMoneff.isEmpty() || rateOptTransferGo.isEmpty()) {
                    execute(new SendMessage(chatId.toString(), "Sorry, could not fetch rates now."));
                    return;
                }

                List<RateInfo> ratesList = new ArrayList<>();
                ratesList.add(new RateInfo("Profee", rateOptProfee.get().rate(), amountGBP.multiply(rateOptProfee.get().rate())));
                ratesList.add(new RateInfo("Moneff", rateOptMoneff.get().rate(), amountGBP.multiply(rateOptMoneff.get().rate())));
                ratesList.add(new RateInfo("TransferGo", rateOptTransferGo.get().rate(), amountGBP.multiply(rateOptTransferGo.get().rate())));
                ratesList.add(new RateInfo("PaySend", rateOptPaysend.get().rate(), amountGBP.multiply(rateOptPaysend.get().rate())));
                //ratesList.add(new RateInfo("Xe", rateOptXe.get().rate(), amountGBP.multiply(rateOptXe.get().rate())));

                // Sort by highest rate first
                ratesList.sort(Comparator.comparing((RateInfo r) -> r.rate).reversed());
                DecimalFormat formatter = new DecimalFormat("#,###.00");
                StringBuilder sb = new StringBuilder();
                for (RateInfo r : ratesList) {
                    sb.append(String.format(
                            "*%s:*\nAmount sent: GBP %s\nExchange rate: %s UZS\nYou receive: %s UZS\n\n",
                            r.name,
                            formatter.format(amountGBP),            // Format GBP amount
                            formatter.format(r.rate),               // Format exchange rate
                            formatter.format(r.totalReceived)
                    ));
                }

                SendMessage response = new SendMessage(chatId.toString(), sb.toString().trim());
                response.setParseMode("Markdown");
                response.disableWebPagePreview();
                execute(response);

            } catch (NumberFormatException e) {
                sendText(chatId, "Please send a valid numeric amount in GBP.");
            } catch (Exception e) {
                sendText(chatId, "An error occurred: " + e.getMessage());
            }
        }
    }

    private void sendText(Long chatId, String text) {
        try {
            execute(new SendMessage(chatId.toString(), text));
        } catch (TelegramApiException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override public String getBotUsername() { return botUsername; }
    @Override public String getBotToken() { return botToken; }
}
