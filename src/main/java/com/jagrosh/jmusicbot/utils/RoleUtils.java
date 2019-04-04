package com.jagrosh.jmusicbot.utils;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

import java.util.Comparator;
import java.util.Optional;

public class RoleUtils {

    public static int getHighestRolePosition(Member member) {
        Optional<Integer> i = member.getRoles().stream()
                .map(Role::getPosition)
                .max(Comparator.naturalOrder());
        if (i.isPresent()) {
            return i.get();
        } else {
            return 0;
        }
    }

    public static boolean hasHigherRoleThanBot(Member member) {
        int memberPosition = getHighestRolePosition(member);
        int botPosition = getHighestRolePosition(member.getGuild().getSelfMember());
        return memberPosition > botPosition;
    }
}