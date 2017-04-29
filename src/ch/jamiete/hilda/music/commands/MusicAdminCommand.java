package ch.jamiete.hilda.music.commands;

import java.util.List;
import com.google.gson.JsonElement;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.Util;
import ch.jamiete.hilda.commands.ChannelCommand;
import ch.jamiete.hilda.configuration.Configuration;
import ch.jamiete.hilda.music.MusicManager;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;

public class MusicAdminCommand extends ChannelCommand {
    private final MusicManager manager;

    public MusicAdminCommand(final Hilda hilda, final MusicManager manager) {
        super(hilda);

        this.manager = manager;

        this.setName("admin");
        this.setDescription("Allows modification of configuration options.");
        this.setMinimumPermission(Permission.MANAGE_SERVER);
    }

    @Override
    public void execute(final Message message, final String[] args, final String label) {
        Configuration config = this.hilda.getConfigurationManager().getConfiguration(this.manager.getPlugin(), message.getGuild().getId());

        if (args.length == 0) {
            this.usage(message, "<output/lock>", label);
            return;
        }

        if (args[0].equalsIgnoreCase("output")) {
            if (args.length == 1) { // Provide current value
                JsonElement output = config.get().get("output");

                if (output == null) {
                    this.reply(message, "There is no output channel currently forced.");
                } else {
                    TextChannel channel = message.getGuild().getTextChannelById(output.getAsString());

                    if (channel == null) {
                        config.get().remove("output");
                        config.save();
                        this.reply(message, "The output channel specified no longer exists. I've removed it.");
                    } else {
                        this.reply(message, "I'm currently outputting into " + channel.getAsMention());
                    }
                }
            } else {
                if (message.getMentionedChannels().isEmpty()) {
                    this.reply(message, "Please mention the channel you want me to output into.");
                } else {
                    TextChannel channel = message.getMentionedChannels().get(0);

                    config.get().addProperty("output", channel.getId());
                    config.save();
                    this.reply(message, "I'm now outputting into " + channel.getAsMention());
                }
            }
        }

        if (args[0].equalsIgnoreCase("lock")) {
            if (args.length == 1) { // Provide current value
                JsonElement output = config.get().get("lock");

                if (output == null) {
                    this.reply(message, "There is no voice channel currently forced.");
                } else {
                    VoiceChannel channel = message.getGuild().getVoiceChannelById(output.getAsString());

                    if (channel == null) {
                        config.get().remove("lock");
                        config.save();
                        this.reply(message, "The voice channel specified no longer exists. I've removed it.");
                    } else {
                        this.reply(message, "I'm currently locked to " + channel.getName());
                    }
                }
            } else {
                List<VoiceChannel> channels = message.getGuild().getVoiceChannelsByName(Util.combineSplit(1, args, " "), true);

                if (channels.isEmpty()) {
                    this.reply(message, "I couldn't find any channels matching that name.");
                } else {
                    VoiceChannel channel = channels.get(0);

                    config.get().addProperty("lock", channel.getId());
                    config.save();
                    this.reply(message, "I'm now locked to " + channel.getName());
                }
            }
        }
    }
}