package dasturlash.uz;

import dasturlash.uz.bot.MyTelegramBot;
import dasturlash.uz.dto.RateDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TelegramPublisher {
    @Value("${telegram.channel.id}")
    private String channelId;

    DecimalFormat formatter = new DecimalFormat("#,###.00");
    private final MyTelegramBot bot;

    public TelegramPublisher(MyTelegramBot bot) {
        this.bot = bot;
    }

    public void publish(List<RateDto> rates) {
        StringBuilder sb = new StringBuilder("💱 GBP → UZS Live Rates\n\n");

        // Sort descending by rate
        rates.sort((r1, r2) -> r2.rate().compareTo(r1.rate()));

        // Append each provider with bold name and formatted rate
        rates.forEach(r -> sb.append(String.format("• *%s*: %s UZS\n",
                r.provider(), formatter.format(r.rate()))));
        // Format timestamp without seconds
        String formattedTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
        sb.append("\nUpdated: ").append(formattedTime);

        // Send as Markdown
        SendMessage msg = new SendMessage(channelId, sb.toString());
        msg.setParseMode("Markdown");
        try {
            bot.execute(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

