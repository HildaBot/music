package ch.jamiete.hilda.music.commands;

import java.util.Arrays;
import java.util.List;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.commands.ChannelSubCommand;
import ch.jamiete.hilda.music.MusicManager;
import ch.jamiete.hilda.music.MusicServer;
import ch.jamiete.hilda.music.QueueItem;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

public class MusicRemoveCommand extends ChannelSubCommand {
    private final MusicManager manager;

    public MusicRemoveCommand(final Hilda hilda, final ChannelSeniorCommand senior, final MusicManager manager) {
        super(hilda, senior);

        this.manager = manager;

        this.setName("remove");
        this.setAliases(Arrays.asList(new String[] { "unqueue" }));
        this.setDescription("Removes a song from the queue.");
    }

    @Override
    public void execute(final Message message, final String[] args, final String label) {
        final Member member = message.getGuild().getMember(message.getAuthor());
        final MusicServer server = this.manager.getServer(message.getGuild());

        if (server == null) {
            this.reply(message, "There isn't anything playing.");
            return;
        }

        final List<QueueItem> queue = server.getQueue();

        if (member.getVoiceState().getChannel() != server.getChannel() && !this.manager.isDJ(message)) {
            MusicManager.getLogger().fine("Rejected command because user not in my voice channel");
            this.reply(message, "You must be in the same voice channel as me to skip.");
            return;
        }

        if (queue.size() == 0) {
            MusicManager.getLogger().fine("Rejected command because no tracks queued");
            this.reply(message, "There isn't anything queued.");
            return;
        }

        int to_remove = -1;

        try {
            to_remove = Integer.parseInt(args[0]) - 1;
        } catch (Exception e) {
            this.usage(message, "<queue_code>", label);
        }

        if (to_remove < 0 || to_remove > queue.size()) {
            this.reply(message, "There is no song with that queue code.");
            return;
        }

        QueueItem item = queue.get(to_remove);

        if (!item.getUserId().equals(message.getAuthor().getId()) && !this.manager.isDJ(message)) {
            this.reply(message, "You can't remove a song you didn't queue.");
            return;
        }

        server.unqueue(item);
        this.reply(message, "OK, I've removed " + MusicManager.getFriendly(item.getTrack()) + " from the queue!");
    }

}