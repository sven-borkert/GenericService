package net.borkert.util;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface GenericService<DTO, ID extends Serializable> {
  public List<DTO> getAll();
  public DTO getById(ID id);
  public DTO addOrUpdate(DTO dto);
  public DTO addOrUpdate(DTO dto, Set remove);
  public void delete(ID id);
  public void deleteIfExists(ID id);
  public void delete(ID[] id);
  public List<DTO> findByProperty(String property, Object value);
  public DTO findOneByProperty(String property, Object value);
  public List<DTO> findByProperty(String property, Object value, int limit);
  public List<DTO> findByProperty(String property, Object value, int limit, int offset);
  public List<DTO> load(int first, int pageSize, String sortField, boolean sortOrder, Map<String, Object> filters);
  public List<DTO> load(int first, int pageSize, String sortField, boolean sortOrder, Map<String, Object> filters, String dateProperty, Date from, Date to);
  public int loadMaxCount(Map<String, Object> filters);
}
