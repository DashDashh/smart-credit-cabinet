package com.hac.cabinet.model.application;

import com.hac.cabinet.model.account.Role;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@Getter
public enum ApplicationStatus {
  PENDING(true),
  APPROVED(false),
  REJECTED(false),
  ACTIVE(false),
  CLOSED(false);

  private final boolean canBeUpdated;

  public boolean canTransitTo(ApplicationStatus to) {
    return this.getNormalTransitionsFrom().contains(to);
  }

  public boolean canTransitToByRole(Role role){
    return getPossibleTransitionsForRole(role).contains(this);
  }

  private Set<ApplicationStatus> getNormalTransitionsFrom() {
    return switch (this) {
      case PENDING -> Set.of(APPROVED, REJECTED, CLOSED);
      case APPROVED -> Set.of(ACTIVE);
      case REJECTED -> Set.of(ACTIVE);
      case ACTIVE -> Set.of();
      case CLOSED -> Set.of();
    };
  }

  private Set<ApplicationStatus> getPossibleTransitionsForRole(Role role) {
    Set<ApplicationStatus> result = HashSet.newHashSet(5);

    if (role.isCanReachVerdict()) {
      result.add(APPROVED);
      result.add(REJECTED);
    }
    if (role.isCanCloseApplication()) {
      result.add(CLOSED);
    }
    if (role.isCanCreateApplication()) {
      result.add(ACTIVE);
    }

    return result;
  }
}
