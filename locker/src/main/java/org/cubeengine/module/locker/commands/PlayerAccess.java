package org.cubeengine.module.locker.commands;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class PlayerAccess
{
    public final User user;
    public final boolean admin;
    public final boolean add;

    public PlayerAccess(User user, boolean admin, boolean add)
    {
        this.user = user;
        this.admin = admin;
        this.add = add;
    }

    public static class PlayerAccessReader implements ArgumentReader<PlayerAccess>, Completer
    {
        private Game game;

        public PlayerAccessReader(Game game)
        {
            this.game = game;
        }

        @Override
        public PlayerAccess read(Class clazz, CommandInvocation invocation) throws ReaderException
        {
            String token = invocation.currentToken();
            boolean admin = token.startsWith("@");
            boolean add = !token.startsWith("-");

            token = admin || add ? token.substring(1) : token;

            User user = game.getServer().getPlayer(token)
                    .map(User.class::cast)
                    .orElse(game.getServiceManager().provideUnchecked(UserStorage.class).get(token)
                            .orElse(null));

            if (user == null)
            {
                throw new IllegalArgumentException("User not found");
            }

            return new PlayerAccess(user, admin, add);
        }

        @Override
        public List<String> getSuggestions(CommandInvocation invocation)
        {
            List<String> list = new ArrayList<>();
            String[] parts = invocation.currentToken().split(",");

            String token = parts[parts.length - 1];

            if (token.isEmpty())
            {
                list.add(invocation.currentToken() + "-");
                list.add(invocation.currentToken() + "@");
            }
            else
            {
                String join = String.join(",", Arrays.copyOfRange(parts, 0, parts.length - 2)) + ",";

                String prefix = "";
                if (token.startsWith("-") || token.startsWith("@"))
                {
                    prefix = token.substring(0, 1);
                    token = token.substring(1);
                }

                final String name = token;
                final String pre = prefix;
                list.addAll(game.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(p -> p.startsWith(name))
                        .map(p -> join + pre + p)
                        .collect(toList()));
            }

            return list;
        }
    }
}
