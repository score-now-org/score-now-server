package com.scorenow.scorenow_api.global.config;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

@Configuration
@ConditionalOnProperty(
	name = "firebase.enabled",
	havingValue = "true"
)
public class FirebaseConfig {

	@Bean
	public FirebaseApp firebaseApp() throws IOException {

		if (!FirebaseApp.getApps().isEmpty()) {
			return FirebaseApp.getInstance();
		}

		FirebaseOptions options = FirebaseOptions.builder()
			.setCredentials(GoogleCredentials.getApplicationDefault())
			.build();

		return FirebaseApp.initializeApp(options);
	}

	@Bean
	public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
		return FirebaseMessaging.getInstance(firebaseApp);
	}
}
