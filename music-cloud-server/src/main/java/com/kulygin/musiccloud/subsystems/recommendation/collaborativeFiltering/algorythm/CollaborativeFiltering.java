package com.kulygin.musiccloud.subsystems.recommendation.collaborativeFiltering.algorythm;

import com.kulygin.musiccloud.domain.Track;
import com.kulygin.musiccloud.service.TrackService;
import com.kulygin.musiccloud.subsystems.recommendation.collaborativeFiltering.config.CFilteringFactory;
import com.kulygin.musiccloud.subsystems.recommendation.collaborativeFiltering.similarity.MeasureOfSimilarity;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

@Component
@Log4j
public class CollaborativeFiltering {
    private MeasureOfSimilarity metric;
    private Map<Integer, Map<Integer, Integer>> userRates;
    @Autowired
    private TrackService trackService;
    private boolean isPrint;

    public void setSettings(CFilteringFactory cFilteringFactory, boolean isPrint) {
        this.metric = cFilteringFactory.createMeasureOfSimilarity();
        this.userRates = cFilteringFactory.createData();
        this.isPrint = isPrint;
    }

    public void makeRecommendation(Integer userID, Integer nBestUsers, Integer nBestProducts) {
        final Map<Integer, Double> similarityProducts = new HashMap<>();

        calculateRecommend(userID, nBestUsers, nBestProducts, similarityProducts);

        if (isPrint) {
            log.info("Most correlated products: ");
            similarityProducts.entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .limit(nBestProducts)
                    .forEach(similarityProduct -> log.info("  TrackID: " + similarityProduct.getKey() + "  Correlation coefficient: " + similarityProduct.getValue()));
        }
    }

    public List<Track> makeRecommendationAndGetTracks(Integer userID, Integer nBestUsers, Integer nBestProducts) {
        final Map<Integer, Double> similarityProducts = new HashMap<>();

        calculateRecommend(userID, nBestUsers, nBestProducts, similarityProducts);

        return trackService.findAllByIds(similarityProducts.entrySet().stream()
                .map(match -> match.getKey().longValue())
                .limit(nBestProducts)
                .collect(Collectors.toList()));
    }

    private void calculateRecommend(Integer userID, Integer nBestUsers, Integer nBestProducts, Map<Integer, Double> similarityProducts) {
        Map<Integer, Double> matches = new HashMap<>();
        /** Choose L users whose tastes are most similar to the tastes of the subject.
         To do this, for each user, you need to calculate the selected measure in relation to
         the user, and choose L largest */

        userRates.forEach((user, value) -> {
            if (!user.equals(userID) && userRates.get(userID) != null) {
                matches.put(user, metric.calculate(userRates.get(userID), userRates.get(user)));
            }
        });

        if (isPrint) {
            log.info("Most correlated '" + userID + "' with users:");
            matches.entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .limit(nBestUsers)
                    .forEach(match -> log.info("  UserID: " + match.getKey() + "  Coefficient: " + match.getValue()));
        }

        Map<Integer, Double> bestMatches = matches.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(nBestUsers)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        /** For each user multiply his estimates by the calculated value of the measure,
         * thus, the evaluation of more "similar" users will have a stronger impact on the final
         * position of the product. For each of the products calculate the sum of the calibrated estimates L
         * * the closest users received amount divided by the amount of measures L selected users. */
        double sum = bestMatches.entrySet().stream().flatMapToDouble(match -> DoubleStream.of(match.getValue())).sum();

        final Map<Integer, Double> resultBestMatches = bestMatches.entrySet().stream()
                .filter(match -> match.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        resultBestMatches.forEach((relatedUser, relatedUserValue) ->
                userRates.get(relatedUser).forEach((product, productValue) -> {
                    if (!userRates.get(userID).containsKey(product)) {
                        similarityProducts.putIfAbsent(product, 0.0d);
                        similarityProducts.put(product, (similarityProducts.get(product) + (userRates.get(relatedUser).get(product) * resultBestMatches.get(relatedUser))) / sum);
                    }
                }));
    }
}
