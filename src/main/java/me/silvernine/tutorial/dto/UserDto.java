package me.silvernine.tutorial.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import me.silvernine.tutorial.entity.User;

import java.util.Set;
import java.util.stream.Collectors;

public record UserDto(

        @NotNull
        @Size(min = 3, max = 50)
        String username,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotNull
        @Size(min = 3, max = 100)
        String password,

        @NotNull
        @Size(min = 3, max = 50)
        String nickname,

        Set<AuthorityDto> authorityDtoSet
) {

    public static UserDto from(User user) {
        if (user == null) {
            return null;
        }

        return new UserDto(
                user.getUsername(),
                null,
                user.getNickname(),
                user.getAuthorities().stream()
                        .map(authority -> new AuthorityDto(authority.getAuthorityName()))
                        .collect(Collectors.toSet())
        );
    }
}
