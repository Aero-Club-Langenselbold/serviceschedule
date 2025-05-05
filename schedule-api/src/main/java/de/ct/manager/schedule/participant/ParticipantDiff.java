package de.ct.manager.schedule.participant;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ParticipantDiff {
    private List<Participant> newParticipants = new ArrayList<>();
    private List<Participant> updatedParticipants = new ArrayList<>();
    private List<Participant> unchangedParticipants = new ArrayList<>();
    private List<Participant> missingParticipants = new ArrayList<>();
}
