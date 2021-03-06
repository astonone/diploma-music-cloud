package com.kulygin.musiccloud.dto;

import com.kulygin.musiccloud.domain.Mood;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.hibernate.validator.internal.util.CollectionHelper.newHashSet;

@Getter
@Setter
public class AllMoodsDTO {

    private Set<MoodDTO> moods;

    public AllMoodsDTO() {
    }

    public AllMoodsDTO(Collection<Mood> dbModel) {

        if (dbModel == null) {
            return;
        }

        this.moods = ofNullable(dbModel).orElse(newHashSet()).stream()
                .map(MoodDTO::new)
                .collect(Collectors.toSet());
    }
}
