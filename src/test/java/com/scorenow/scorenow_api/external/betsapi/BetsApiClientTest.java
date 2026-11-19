package com.scorenow.scorenow_api.external.betsapi;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsLineupResponse;

@ExtendWith(MockitoExtension.class)
class BetsApiClientTest {

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private BetsApiProperties properties;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private BetsApiClient client;

	@BeforeEach
	void setUp() {
		client = new BetsApiClient(
			restTemplate,
			properties,
			objectMapper
		);
	}

	@Test
	void getLineup_emptyResults() throws Exception {
		// given
		when(properties.getBaseUrl())
			.thenReturn("https://api.betsapi.com");
		when(properties.getToken())
			.thenReturn("test-token");

		JsonNode response = objectMapper.readTree("""
			{
			    "success": 1,
			    "results": []
			}
			""");

		when(restTemplate.getForObject(
			anyString(),
			eq(JsonNode.class)
		)).thenReturn(response);

		// when
		BetsLineupResponse result = client.getLineup("12345");

		// then
		assertThat(result.getSuccess()).isEqualTo(1);
		assertThat(result.getResults()).isNotNull();
		assertThat(result.getResults().getHome()).isNull();
		assertThat(result.getResults().getAway()).isNull();

		verify(restTemplate).getForObject(
			contains("/v4/event/lineup"),
			eq(JsonNode.class)
		);
	}

	@Test
	void getLineup_normalResults() throws Exception {
		// given
		when(properties.getBaseUrl())
			.thenReturn("https://api.betsapi.com");
		when(properties.getToken())
			.thenReturn("test-token");

		JsonNode response = objectMapper.readTree("""
			{
			    "success": 1,
			    "results": {
			        "home": {
			            "formation": "4-3-3",
			            "startinglineup": [
			                {
			                    "player": {
			                        "id": "100",
			                        "name": "Test Player"
			                    },
			                    "shirtnumber": "7",
			                    "pos": "Forward"
			                }
			            ],
			            "substitutes": []
			        },
			        "away": {
			            "formation": "4-4-2",
			            "startinglineup": [],
			            "substitutes": []
			        }
			    }
			}
			""");

		when(restTemplate.getForObject(
			anyString(),
			eq(JsonNode.class)
		)).thenReturn(response);

		// when
		BetsLineupResponse result = client.getLineup("12345");

		// then
		assertThat(result.getSuccess()).isEqualTo(1);
		assertThat(result.getResults().getHome().getFormation())
			.isEqualTo("4-3-3");
		assertThat(result.getResults().getAway().getFormation())
			.isEqualTo("4-4-2");

		assertThat(result.getResults().getHome().getStartinglineup())
			.hasSize(1);

		assertThat(result.getResults().getHome()
			.getStartinglineup().get(0).getPlayer().getId())
			.isEqualTo("100");
	}
}
