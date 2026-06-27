package me.silvernine.tutorial.service;

import me.silvernine.tutorial.dto.UserDto;
import me.silvernine.tutorial.exception.DuplicateMemberException;
import me.silvernine.tutorial.exception.NotFoundMemberException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    private UserDto newUser(String username) {
        return new UserDto(username, "password123", username, null);
    }

    @Test
    void signup_persistsUserWithRoleUser() {
        UserDto saved = userService.signup(newUser("dave"));

        assertThat(saved.username()).isEqualTo("dave");
        assertThat(saved.password()).isNull();
        assertThat(saved.authorityDtoSet())
                .extracting("authorityName")
                .containsExactly("ROLE_USER");
    }

    @Test
    void signup_duplicate_throwsDuplicateMember() {
        userService.signup(newUser("erin"));

        assertThatThrownBy(() -> userService.signup(newUser("erin")))
                .isInstanceOf(DuplicateMemberException.class);
    }

    @Test
    void getUserWithAuthorities_notFound_throwsNotFoundMember() {
        assertThatThrownBy(() -> userService.getUserWithAuthorities("no-such-user"))
                .isInstanceOf(NotFoundMemberException.class);
    }
}
