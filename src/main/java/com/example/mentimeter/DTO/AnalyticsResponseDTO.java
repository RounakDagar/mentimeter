package com.example.mentimeter.DTO;

import com.example.mentimeter.Model.QuestionAnalytics;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponseDTO {
    private List<QuestionAnalytics> questions;
    private List<LeaderboardEntryDTO> leaderboard;
}
    