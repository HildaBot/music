package ch.jamiete.hilda.music;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.commands.ChannelCommand;
import net.dv8tion.jda.core.entities.Message;

public class MusicQueueCommand extends ChannelCommand {
    private final MusicManager manager;

    public MusicQueueCommand(final Hilda hilda, final MusicManager manager) {
        super(hilda);

        this.manager = manager;

        this.setName("queue");
        this.setAliases(Arrays.asList(new String[] { "page", "q" }));
        this.setDescription("Lists the current queue.");
    }

    @Override
    public void execute(final Message message, final String[] args, final String label) {
        final MusicServer server = this.manager.getServer(message.getGuild());

        if (server == null) {
            this.reply(message, "There isn't anything playing.");
            return;
        }

        int page = 0;
        final int pageSize = 15;

        if (args.length == 1) {
            if (StringUtils.isNumeric(args[0])) {
                page = Integer.valueOf(args[0]) - 1;
            } else {
                this.usage(message, "[page]", label);
                return;
            }
        }

        final List<QueueItem> queue = server.getQueue();

        if (queue.isEmpty()) {
            this.reply(message, "There isn't anything queued.");
            return;
        }

        final List<QueueItem> tracks = this.getPage(queue, page, pageSize);
        final StringBuilder sb = new StringBuilder();

        if (tracks.isEmpty()) {
            this.reply(message, "That page is empty.");
        } else {
            sb.append("There ").append(queue.size() == 1 ? "is" : "are").append(" ");
            sb.append(queue.size()).append(" ").append(queue.size() == 1 ? "track" : "tracks");
            sb.append(" queued");

            long ms = 0;
            for (final QueueItem track : tracks) {
                ms += track.getTrack().getDuration();
            }
            sb.append(" for ").append(MusicManager.getFriendlyTime(ms));

            if (tracks.size() != queue.size()) {
                sb.append("; showing tracks ");

                if (page == 0) {
                    sb.append("1–").append(pageSize);
                } else {
                    final int first = page * pageSize + 1;
                    sb.append(first).append("–").append(Math.min(first + pageSize, queue.size()) + 1);
                }
            }

            sb.append(":").append("\n\n");

            for (final QueueItem track : tracks) {
                sb.append(MusicManager.getFriendly(track.getTrack()));

                final String time = MusicManager.getFriendlyTime(track.getTrack());
                if (time.trim().length() > 0) {
                    sb.append(" (" + time + ")");
                }

                sb.append(" `").append(message.getGuild().getMemberById(track.getUserId()).getEffectiveName()).append("`");

                sb.append("\n");
            }

            if (tracks.size() != queue.size()) {
                sb.append("\n");
                sb.append("End of page ").append(page + 1).append("/").append((int) Math.ceil((double) queue.size() / pageSize)).append(".");
            }

            this.reply(message, sb.toString());
        }
    }

    private <T> List<T> getPage(final List<T> sourceList, final int page, final int pageSize) {
        final int fromIndex = page * pageSize;

        if (sourceList == null || sourceList.size() < fromIndex) {
            return Collections.emptyList();
        }

        return sourceList.subList(fromIndex, Math.min(fromIndex + pageSize, sourceList.size()));
    }

}
