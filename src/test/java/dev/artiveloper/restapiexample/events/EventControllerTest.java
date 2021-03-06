package dev.artiveloper.restapiexample.events;

import dev.artiveloper.restapiexample.accounts.Account;
import dev.artiveloper.restapiexample.accounts.AccountRepository;
import dev.artiveloper.restapiexample.accounts.AccountRole;
import dev.artiveloper.restapiexample.accounts.AccountService;
import dev.artiveloper.restapiexample.common.BaseControllerTest;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.util.Jackson2JsonParser;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.IntStream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EventControllerTest extends BaseControllerTest {

    @Autowired
    EventRepositoy eventRepositoy;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AccountService accountService;

    @Before
    public void setUp() throws Exception {
        this.eventRepositoy.deleteAll();
        this.accountRepository.deleteAll();
    }

    @Test
    public void createEvent() throws Exception {
        String api = "/api/events";

        EventDto event = EventDto.builder()
                .name("Spring")
                .description("REST API Development with Spring")
                .beginEnrollmentDateTime(LocalDateTime.of(2018, 11, 23, 14, 21))
                .closeEnrollmentDateTime(LocalDateTime.of(2018, 11, 24, 14, 21))
                .beginEventDateTime(LocalDateTime.of(2018, 11, 25, 14, 21))
                .endEventDateTime(LocalDateTime.of(2018, 11, 26, 14, 21))
                .basePrice(100)
                .maxPrice(200)
                .limitOfEnrollment(100)
                .location("강남역 D2 스타텁 팩토리")
                .build();

        mockMvc.perform(
                post(api)
                        .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaTypes.HAL_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id").exists())
                .andExpect(jsonPath("id").value(Matchers.not(100)))
                .andExpect(jsonPath("free").value(false))
                .andExpect(jsonPath("offline").value(true))
                .andExpect(jsonPath("eventStatus").value(EventStatus.DTAFT.name()))
                .andExpect(jsonPath("_links.self").exists())
                .andExpect(jsonPath("_links.query-events").exists())
                .andExpect(jsonPath("_links.update-event").exists());
    }

    private String getBearerToken() throws Exception {
        return "Bearer " + getAccessToken();
    }

    private String getAccessToken() throws Exception {
        String api = "/oauth/token";
        String clientId = "myApp";
        String clientSecret = "pass";
        String username = "artiveloper@gmail.com";
        String password = "password";

        Account account = Account.builder()
                .email(username)
                .password(password)
                .roles(new HashSet<>(Arrays.asList(AccountRole.ADMIN, AccountRole.USER)))
                .build();

        this.accountService.save(account);

        ResultActions perform = this.mockMvc.perform(post(api)
                .with(httpBasic(clientId, clientSecret))
                .param("username", username)
                .param("password", password)
                .param("grant_type", "password"));

        String response = perform.andReturn().getResponse().getContentAsString();
        return new Jackson2JsonParser().parseMap(response).get("access_token").toString();
    }

    @Test
    public void createEvent_BadRequest() throws Exception {
        String api = "/api/events";

        Event event = Event.builder()
                .id(100)
                .name("spring")
                .description("REST API Developerment with Spring")
                .beginEnrollmentDateTime(LocalDateTime.of(2018, 11, 11, 0, 0))
                .closeEnrollmentDateTime(LocalDateTime.of(2018, 11, 15, 0, 0))
                .beginEventDateTime(LocalDateTime.of(2018, 11, 25, 0, 0))
                .basePrice(100)
                .maxPrice(200)
                .limitOfEnrollment(100)
                .location("강남역 D2 스타텁 팩토리")
                .free(true)
                .eventStatus(EventStatus.PUBLISHED)
                .build();

        mockMvc.perform(
                post(api)
                        .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaTypes.HAL_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createEvent_BadRequest_InputValid() throws Exception {
        String api = "/api/events";

        EventDto eventDto = EventDto.builder()
                .name("Spring")
                .description("REST API Development with Spring")
                .beginEnrollmentDateTime(LocalDateTime.of(2018, 11, 26, 14, 21))
                .closeEnrollmentDateTime(LocalDateTime.of(2018, 11, 25, 14, 21))
                .beginEventDateTime(LocalDateTime.of(2018, 11, 24, 14, 21))
                .endEventDateTime(LocalDateTime.of(2018, 11, 23, 14, 21))
                .basePrice(10000)
                .maxPrice(200)
                .limitOfEnrollment(100)
                .location("강남역 D2 스타텁 팩토리")
                .build();

        this.mockMvc.perform(post("/api/events")
                .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(this.objectMapper.writeValueAsString(eventDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("content[0].objectName").exists())
                .andExpect(jsonPath("content[0].defaultMessage").exists())
                .andExpect(jsonPath("content[0].code").exists())
                .andExpect(jsonPath("_links.index").exists());
    }

    //이벤트 30개를 10개씩 두번째 페이지 조회하기
    @Test
    public void queryEvents() throws Exception {
        //given
        IntStream.range(0, 30).forEach(this::generateEvent);

        String api = "/api/events";
        //when
        this.mockMvc.perform(get(api)
                .param("page", "1")
                .param("size", "10")
                .param("sort", "name,DESC")
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("page").exists())
                .andExpect(jsonPath("_embedded.eventList[0]_links.self").exists());
    }

    private Event generateEvent(int index) {
        Event event = Event.builder()
                .name("event " + index)
                .description("test event")
                .beginEnrollmentDateTime(LocalDateTime.of(2018, 11, 23, 14, 21))
                .closeEnrollmentDateTime(LocalDateTime.of(2018, 11, 24, 14, 21))
                .beginEventDateTime(LocalDateTime.of(2018, 11, 25, 14, 21))
                .endEventDateTime(LocalDateTime.of(2018, 11, 26, 14, 21))
                .basePrice(100)
                .maxPrice(200)
                .limitOfEnrollment(100)
                .location("강남역 D2 스타텁 팩토리")
                .free(false)
                .offline(true)
                .eventStatus(EventStatus.DTAFT)
                .build();

        return this.eventRepositoy.save(event);
    }

    @Test
    public void getEvent() throws Exception {
        Event event = this.generateEvent(100);

        String api = "/api/events/{id}";
        this.mockMvc.perform(get(api, event.getId()))
                .andDo(print())
                .andExpect(jsonPath("name").exists())
                .andExpect(jsonPath("id").exists())
                .andExpect(jsonPath("_links.self").exists());
        //.andExpect(jsonPath("_links.profile").exists());
    }

    @Test
    public void getEvent_404() throws Exception {
        String api = "/api/events/12341234";
        this.mockMvc.perform(get(api))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateEvent() throws Exception {
        String api = "/api/events/{id}";
        Event createdEvent = generateEvent(100);

        EventDto updatedEvent = this.modelMapper.map(createdEvent, EventDto.class);
        String updatedName = "업데이트된 이벤트";
        String updatedDescription = "REST API Development with Spring";

        updatedEvent.setName(updatedName);
        updatedEvent.setDescription(updatedDescription);

        this.mockMvc.perform(
                put(api, createdEvent.getId())
                        .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaTypes.HAL_JSON)
                        .content(objectMapper.writeValueAsString(updatedEvent)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("name").value(updatedName))
                .andExpect(jsonPath("description").value(updatedDescription))
                .andExpect(jsonPath("_links.self").exists());
    }

    @Test
    public void updateEvent_badRequest_emptyInputValue() throws Exception {
        String api = "/api/events/{id}";
        Event createdEvent = generateEvent(100);

        EventDto updatedEvent = new EventDto();

        this.mockMvc.perform(
                put(api, createdEvent.getId())
                        .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaTypes.HAL_JSON)
                        .content(objectMapper.writeValueAsString(updatedEvent)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateEvent_badRequest_wrongInputValue() throws Exception {
        String api = "/api/events/{id}";
        Event createdEvent = generateEvent(100);

        EventDto updatedEvent = modelMapper.map(createdEvent, EventDto.class);
        updatedEvent.setBasePrice(2000);
        updatedEvent.setMaxPrice(1000);

        this.mockMvc.perform(
                put(api, createdEvent.getId())
                        .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaTypes.HAL_JSON)
                        .content(objectMapper.writeValueAsString(updatedEvent)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateEvent_notFound() throws Exception {
        String api = "/api/events/12341234";
        Event createdEvent = generateEvent(100);

        EventDto updatedEvent = modelMapper.map(createdEvent, EventDto.class);

        this.mockMvc.perform(
                put(api)
                        .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaTypes.HAL_JSON)
                        .content(objectMapper.writeValueAsString(updatedEvent)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

}
