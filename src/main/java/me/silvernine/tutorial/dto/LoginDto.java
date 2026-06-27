package me.silvernine.tutorial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginDto(

        @NotNull
        @Size(min = 3, max = 50)
        String username,

        @NotNull
        @Size(min = 3, max = 100)
        String password
) {
}
