package net.borkert.util;

public interface DomainEntity<DTO> {
  public DTO toDTO();
  public void fromDTO(DTO dto);
}
