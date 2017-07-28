/*******************************************************************************
 * Copyright 2017 jamietech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package ch.jamiete.hilda.music.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.Util;
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.commands.ChannelSubCommand;
import ch.jamiete.hilda.music.MusicManager;
import ch.jamiete.hilda.music.MusicServer;
import ch.jamiete.hilda.music.QueueItem;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.MessageBuilder.Formatting;
import net.dv8tion.jda.core.MessageBuilder.SplitPolicy;
import net.dv8tion.jda.core.entities.Message;

public class MusicQueueCommand extends ChannelSubCommand {
    private final MusicManager manager;

    public MusicQueueCommand(final Hilda hilda, final ChannelSeniorCommand senior, final MusicManager manager) {
        super(hilda, senior);

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
        int queue_code = 0;

        if (args.length == 1) {
            if (StringUtils.isNumeric(args[0])) {
                page = Integer.valueOf(args[0]) - 1;
                queue_code = page * pageSize;
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
        final MessageBuilder sb = new MessageBuilder();

        if (tracks.isEmpty()) {
            this.reply(message, "That page is empty.");
        } else {
            sb.append("There ").append(queue.size() == 1 ? "is" : "are").append(" ");
            sb.append(queue.size()).append(" ").append(queue.size() == 1 ? "track" : "tracks");
            sb.append(" queued for ").append(Util.getFriendlyTime(server.getDuration()));

            if (tracks.size() != queue.size()) {
                sb.append("; showing tracks ");

                if (page == 0) {
                    sb.append("1–").append(pageSize);
                } else {
                    final int first = page * pageSize + 1;
                    sb.append(first).append("–").append(Math.min(first + pageSize - 1, queue.size()));
                }
            }

            sb.append(":").append("\n\n");

            for (final QueueItem track : tracks) {
                sb.append("[" + ++queue_code + "]", Formatting.BLOCK).append(" ");

                sb.append(Util.sanitise(MusicManager.getFriendly(track.getTrack())));

                final String time = MusicManager.getFriendlyTime(track.getTrack());
                if (time.trim().length() > 0) {
                    sb.append(" (" + time + ")");
                }

                sb.append(" ").append(message.getGuild().getMemberById(track.getUserId()).getEffectiveName(), Formatting.BLOCK);

                sb.append("\n");
            }

            if (tracks.size() != queue.size()) {
                sb.append("\n");
                sb.append("End of page ").append(page + 1).append("/").append((int) Math.ceil((double) queue.size() / pageSize)).append(".");
            }

            sb.buildAll(SplitPolicy.NEWLINE).forEach(m -> this.reply(message, m));
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
