package org.tsd.tsdbot.listener.channel;

import com.google.inject.Inject;
import de.btobastian.javacord.DiscordAPI;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.Constants;
import org.tsd.tsdbot.discord.DiscordChannel;
import org.tsd.tsdbot.discord.DiscordMessage;
import org.tsd.tsdbot.history.HistoryCache;
import org.tsd.tsdbot.history.HistoryRequest;
import org.tsd.tsdbot.history.filter.FilterFactory;
import org.tsd.tsdbot.listener.MessageHandler;
import org.tsd.tsdbot.util.MiscUtils;

public class DeejHandler extends MessageHandler<DiscordChannel> {

    private static final Logger log = LoggerFactory.getLogger(DeejHandler.class);

    private final HistoryCache historyCache;
    private final FilterFactory filterFactory;

    @Inject
    public DeejHandler(DiscordAPI api,
                       HistoryCache historyCache,
                       FilterFactory filterFactory) {
        super(api);
        this.historyCache = historyCache;
        this.filterFactory = filterFactory;
    }

    @Override
    public boolean isValid(DiscordMessage<DiscordChannel> message) {
        return StringUtils.equals(message.getContent().trim(), Constants.Deej.PREFIX);
    }

    @Override
    public void doHandle(DiscordMessage<DiscordChannel> message, DiscordChannel channel) throws Exception {
        log.info("Handling deej: channel={}, message={}", message.getRecipient().getName(), message.getContent());

        HistoryRequest<DiscordChannel> request = HistoryRequest.create(channel, message)
                .withFilter(filterFactory.createNoFunctionsFilter())
                .withFilter(filterFactory.createNoOwnMessagesFilter())
                .withFilter(filterFactory.createNoUrlsFilter())
                .withFilter(filterFactory.createNoBotsFilter())
                .withFilter(filterFactory.createIgnorableFilter());

        DiscordMessage<DiscordChannel> random = historyCache.getRandomChannelMessage(request);
        if (random != null) {
            String decorator = MiscUtils.getRandomItemInList(Constants.Deej.DECORATORS);
            if (decorator != null) {
                String result = String.format(decorator, random.getContent());
                log.info("Deejified: {}", result);
                channel.sendMessage(result);
            }
        }
    }
}
