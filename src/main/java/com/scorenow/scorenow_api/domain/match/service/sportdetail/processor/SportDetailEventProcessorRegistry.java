package com.scorenow.scorenow_api.domain.match.service.sportdetail.processor;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SportDetailEventProcessorRegistry {

    private final Map<SportDetailType, SportDetailEventProcessor> eventProcessors;

    public SportDetailEventProcessorRegistry(List<SportDetailEventProcessor> eventProcessorList) {
        this.eventProcessors = eventProcessorList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        SportDetailEventProcessor::type,
                        processor -> processor
                ));
    }

    public void process(
            MatchDetailDocument currentMatchDetailDocument,
            MatchDetailDocument newMatchDetailDocument) {

        SportDetailType type = newMatchDetailDocument.getType();

        SportDetailEventProcessor processor = eventProcessors.get(type);
        if (processor == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "지원하지 않는 종목입니다. type=" + type.name());
        }

        processor.process(currentMatchDetailDocument, newMatchDetailDocument);
    }
}
