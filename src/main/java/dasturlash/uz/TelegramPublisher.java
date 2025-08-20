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
        msg.setParseMode("Markdown");
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
        edit.setParseMode("Markdown");
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

       StringBuilder sb = new StringBuilder("```\n"); // mono format
       sb.append("💱 GBP → UZS Live Rates\n\n");

       // Sort rates descending
       rates.sort((r1, r2) -> r2.rate().compareTo(r1.rate()));

       // Calculate max lengths
       int maxProviderLen = rates.stream()
               .mapToInt(r -> r.provider().length()).max().orElse(1);
       int maxRateLen = rates.stream()
               .mapToInt(r -> formatter.format(r.rate()).length()).max().orElse(1);
       int maxDeltaLen = rates.stream().mapToInt(r -> {
           if (prevRates != null) {
               BigDecimal delta =
                       r.rate().subtract(prevRates.getOrDefault(r.provider(), r.rate()));
               if (delta.compareTo(BigDecimal.ZERO) > 0)
                   return String.format("(🟢 +%s)", formatter.format(delta)).length();
               else if (delta.compareTo(BigDecimal.ZERO) < 0)
                   return String.format("(🔴 -%s)", formatter.format(delta.abs())).length();
           }
           return 0;
       }).max().orElse(0);
       int maxFeeLen = rates.stream().mapToInt(r ->
               getFeeForProvider(r.provider()).length()).max().orElse(1);

       // Ensure widths are at least 1 to avoid %-0s
       maxProviderLen = Math.max(1, maxProviderLen);
       maxRateLen = Math.max(1, maxRateLen);
       maxDeltaLen = Math.max(1, maxDeltaLen);
       maxFeeLen = Math.max(1, maxFeeLen);

       for (RateDto r : rates) {
           String provider = r.provider();
           BigDecimal currentRate = r.rate();
           String feeStr = String.format("Fee: £%s", getFeeForProvider(provider));

           String deltaStr = "";
           if (prevRates != null) {
               BigDecimal prevRate = prevRates.getOrDefault(provider, currentRate);
               BigDecimal delta = currentRate.subtract(prevRate);
               if (delta.compareTo(BigDecimal.ZERO) > 0) deltaStr
                       = String.format("(🟢 +%s UZS)", formatter.format(delta));
               else if (delta.compareTo(BigDecimal.ZERO) < 0) deltaStr
                       = String.format("(🔴 -%s UZS)", formatter.format(delta.abs()));
           }

           sb.append(String.format(
                   "• %-"+maxProviderLen+"s %"+maxRateLen+"s UZS %-"
                           +maxDeltaLen+"s | %-"+maxFeeLen+"s\n",
                   provider,
                   formatter.format(currentRate),
                   deltaStr,
                   feeStr
           ));
       }

       String formattedTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
               .withZone(ZoneId.systemDefault())
               .format(Instant.now());
       sb.append("\nUpdated: ").append(formattedTime).append("\n");
       sb.append("```"); // end mono
       // Add links section
       sb.append("\n\nLinks: ")
               .append("[TransferGo](https://transfergo.com) | ")
               .append("[Moneff](https://qrco.de/bfsv7o) | ")
               .append("[Profee](https://profee.com) | ")
               .append("[Paysend](https://paysend.com)");

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
