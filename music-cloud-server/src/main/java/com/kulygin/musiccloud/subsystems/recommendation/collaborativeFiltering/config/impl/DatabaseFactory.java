package com.kulygin.musiccloud.subsystems.recommendation.collaborativeFiltering.config.impl;

import com.kulygin.musiccloud.domain.StatisticalAccounting;
import com.kulygin.musiccloud.service.StatisticalAccountingService;
import com.kulygin.musiccloud.subsystems.recommendation.collaborativeFiltering.config.CFilteringFactory;
import com.kulygin.musiccloud.subsystems.recommendation.collaborativeFiltering.similarity.MeasureOfSimilarity;
import com.kulygin.musiccloud.subsystems.recommendation.collaborativeFiltering.similarity.impl.CosineMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DatabaseFactory extends CFilteringFactory {

    @Autowired
    private StatisticalAccountingService statisticalAccountingService;

    @Override
    public Map<Integer, Map<Integer, Integer>> createData() {
        Map<Integer, Map<Integer, Integer>> userRates = new HashMap<>();
        List<StatisticalAccounting> all = statisticalAccountingService.findAll();
        all.forEach(line -> {

            Integer userId = line.getUserId().intValue();
            Integer trackId = line.getTrackId().intValue();
            Integer rate = line.getRatingValue();

            if (userRates.containsKey(userId)) {
                Map<Integer, Integer> trackRates = userRates.get(userId);
                trackRates.put(trackId, rate);
            } else {
                Map<Integer, Integer> trackRates = new HashMap<>();
                trackRates.put(trackId, rate);
                userRates.put(userId, trackRates);
            }
        });
        return userRates;
    }

    @Override
    public MeasureOfSimilarity createMeasureOfSimilarity() {
        return new CosineMeasure();
    }
}
