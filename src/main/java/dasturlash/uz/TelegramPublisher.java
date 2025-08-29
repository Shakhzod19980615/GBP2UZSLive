package dasturlash.uz;

import dasturlash.uz.bot.MyTelegramBot;
import dasturlash.uz.dto.RateDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TelegramPublisher {

    @Value("${telegram.channel.id}")
    private String channelId;

    private final MyTelegramBot bot;

    public TelegramPublisher(MyTelegramBot bot) {
        this.bot = bot;
    }

    // Send a new message and return its message ID
    public String publishNew(List<RateDto> rates) {
        String text = formatRatesWithFee(rates, null);
        SendMessage msg = new SendMessage(channelId, text);
        msg.setParseMode("HTML");
        msg.disableWebPagePreview();

        try {
            return bot.execute(msg).getMessageId().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Update existing message by messageId, comparing with previous rates
    public void updateMessage(String messageId, List<RateDto> rates, Map<String, BigDecimal> prevRates) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(channelId);
        edit.setMessageId(Integer.parseInt(messageId));
        edit.setText(formatRatesWithFee(rates, prevRates));
        edit.setParseMode("HTML");
        edit.disableWebPagePreview();

        try {
            bot.execute(edit);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   private String formatRatesWithFee(List<RateDto> rates, Map<String, BigDecimal> prevRates) {
       DecimalFormatSymbols symbols = new DecimalFormatSymbols();
       symbols.setGroupingSeparator(',');
       DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);

       StringBuilder sb = new StringBuilder();
       sb.append("<b>💱 GBP → UZS Live Rates</b>\n\n");
       sb.append("<pre>");

       // sort rates descending
       rates.sort((r1, r2) -> r2.rate().compareTo(r1.rate()));

       for (RateDto r : rates) {
           String provider = r.provider();
           BigDecimal currentRate = r.rate();
           BigDecimal prevRate = prevRates != null
                   ? prevRates.getOrDefault(provider, currentRate)
                   : currentRate;

           BigDecimal delta = currentRate.subtract(prevRate);
           String deltaStr = "";
           if (delta.compareTo(BigDecimal.ZERO) > 0) {
               deltaStr = "(🟢 +" + formatter.format(delta) + " UZS)";
           } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
               deltaStr = "(🔴 -" + formatter.format(delta.abs()) + " UZS)";
           }

           sb.append("• ")
                   .append(provider)
                   .append(" ")
                   .append(formatter.format(currentRate))
                   .append(" UZS ")
                   .append(deltaStr)
                   .append(" | Fee: £")
                   .append(getFeeForProvider(provider))
                   .append("\n");
       }

       sb.append("</pre>\n");

       String formattedTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
               .withZone(ZoneId.systemDefault())
               .format(Instant.now());
       sb.append("\nUpdated: ").append(formattedTime).append("\n\n");

       sb.append("Links: ")
               .append("<a href=\"https://transfergo.com\">TransferGo</a> | ")
               .append("<a href=\"https://qrco.de/bfsv7o\">Moneff</a> | ")
               .append("<a href=\"https://profee.com\">Profee</a> | ")
               .append("<a href=\"https://paysend.com\">Paysend</a>");

       return sb.toString();
   }

    private String getFeeForProvider(String provider) {
        return switch (provider) {
            case "Moneff", "TransferGo" -> "0.00";
            case "Profee", "Paysend" -> "1.00";
            default -> "0.00";
        };
    }


}
