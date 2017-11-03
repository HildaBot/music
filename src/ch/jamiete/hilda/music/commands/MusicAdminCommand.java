/*
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
 */
package ch.jamiete.hilda.music.commands;

import java.util.List;
import com.google.gson.JsonElement;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.Util;
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.commands.ChannelSubCommand;
import ch.jamiete.hilda.configuration.Configuration;
import ch.jamiete.hilda.music.MusicManager;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;

class MusicAdminCommand extends ChannelSubCommand {
    private final MusicManager manager;

    MusicAdminCommand(final Hilda hilda, final ChannelSeniorCommand senior, final MusicManager manager) {
        super(hilda, senior);

        this.manager = manager;

        this.setName("admin");
        this.setDescription("Allows modification of configuration options.");
        this.setMinimumPermission(Permission.MANAGE_SERVER);
    }

    @Override
    public final void execute(final Message message, final String[] args, final String label) {
        final Configuration config = this.hilda.getConfigurationManager().getConfiguration(this.manager.getPlugin(), message.getGuild().getId());

        if (args.length == 0) {
            this.usage(message, "<output/lock>", label);
            return;
        }

        if ("output".equalsIgnoreCase(args[0])) {
            if (args.length == 1) { // Provide current value
                final JsonElement output = config.get().get("output");

                if (output == null) {
                    this.reply(message, "There is no output channel currently forced.");
                } else {
                    final TextChannel channel = message.getGuild().getTextChannelById(output.getAsString());

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
                    final TextChannel channel = message.getMentionedChannels().get(0);

                    config.get().addProperty("output", channel.getId());
                    config.save();
                    this.reply(message, "I'm now outputting into " + channel.getAsMention());
                }
            }
        }

        if ("lock".equalsIgnoreCase(args[0])) {
            if (args.length == 1) { // Provide current value
                final JsonElement output = config.get().get("lock");

                if (output == null) {
                    this.reply(message, "There is no voice channel currently forced.");
                } else {
                    final VoiceChannel channel = message.getGuild().getVoiceChannelById(output.getAsString());

                    if (channel == null) {
                        config.get().remove("lock");
                        config.save();
                        this.reply(message, "The voice channel specified no longer exists. I've removed it.");
                    } else {
                        this.reply(message, "I'm currently locked to " + channel.getName());
                    }
                }
            } else {
                final List<VoiceChannel> channels = message.getGuild().getVoiceChannelsByName(Util.combineSplit(1, args, " "), true);

                if (channels.isEmpty()) {
                    this.reply(message, "I couldn't find any channels matching that name.");
                } else {
                    final VoiceChannel channel = channels.get(0);

                    config.get().addProperty("lock", channel.getId());
                    config.save();
                    this.reply(message, "I'm now locked to " + channel.getName());
                }
            }
        }
    }
}