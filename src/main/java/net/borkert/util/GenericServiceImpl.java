package net.borkert.util;

import net.borkert.persistence.ExtendedSessionFactory;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.*;

public class GenericServiceImpl<DOMAIN, DTO, ID extends Serializable>
    implements GenericService<DTO, ID> {

  private static Logger log = LogManager.getLogger(GenericServiceImpl.class);

  protected final Class<DOMAIN> domainClass;
  protected final Class<DTO> dtoClass;
  protected final HashMap<Class, Class> classMapping = new HashMap<>();

  @Resource
  private TransactionTemplate transactionTemplate;

  private ExtendedSessionFactory esf;

  public GenericServiceImpl() {
    this.domainClass = (Class<DOMAIN>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    this.dtoClass = (Class<DTO>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<DTO> getAll() {
    return toDTO(getEsf().findAll(domainClass));
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public DTO getById(ID id) {
    return toDTO(getEsf().findById(domainClass, id));
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public DTO addOrUpdate(DTO dto) {
    log.debug("addOrUpdate(dto): " + dto.toString());
    final DTO finalEntity = dto;
    Object result = getTransactionTemplate().execute(transactionStatus -> toDTO(getEsf().makePersistent(toDomain(finalEntity))));
    handleUpdate();
    return (DTO) result;
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public DTO addOrUpdate(final DTO dto, Set remove) {
    log.debug("addOrUpdate(dto,remove): " + dto.toString());
    final Set removeSet = remove;
    Object result = getTransactionTemplate().execute(transactionStatus -> {
      for (Object o : removeSet) {
        getEsf().makeTransient(toDomainViaMapping(o));
      }
      return toDTO(getEsf().makePersistent(toDomain(dto)));
    });
    handleUpdate();
    return (DTO) result;
  }

  @Override
  public void delete(ID id) {
    final ID deleteId = id;
    log.debug("delete(): " + domainClass + " : " + id);
    getTransactionTemplate().execute(transactionStatus -> {
      getEsf().makeTransient(getEsf().findById(domainClass, deleteId));
      return null;
    });
    handleUpdate();
  }

  @Override
  public void deleteIfExists(ID id) {
    final ID deleteId = id;
    log.debug("deleteIfExists(): " + domainClass + " : " + id);
    getTransactionTemplate().execute(transactionStatus -> {
      DOMAIN o = getEsf().findById(domainClass, deleteId);
      if (o != null) {
        getEsf().makeTransient(o);
      }
      return null;
    });
    handleUpdate();
  }

  @Override
  public void delete(ID[] id) {
    final ID[] deleteId = id;
    getTransactionTemplate().execute(transactionStatus -> {
      for (ID i : deleteId) {
        log.debug("delete(): " + domainClass + " : " + i);
        getEsf().makeTransient(getEsf().findById(domainClass, i));
      }
      return null;
    });
    handleUpdate();
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<DTO> findByProperty(String property, Object value) {
    return toDTO(getEsf().findByProperty(domainClass, property, value));
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public DTO findOneByProperty(String property, Object value) {
    return toDTO(getEsf().findOneByProperty(domainClass, property, value));
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<DTO> findByProperty(String property, Object value, int limit) {
    return toDTO(getEsf().findByProperty(domainClass, property, value, limit));
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<DTO> findByProperty(String property, Object value, int limit, int offset) {
    return toDTO(getEsf().findByProperty(domainClass, property, value, limit));
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<DTO> load(int first, int pageSize, String sortField, boolean sortOrder, Map<String, Object> filters) {
    return toDTO(getEsf().load(domainClass, first, pageSize, sortField, sortOrder, filters));
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<DTO> load(int first, int pageSize, String sortField, boolean sortOrder, Map<String, Object> filters, String dateProperty, Date from, Date to) {
    return toDTO(getEsf().load(domainClass, first, pageSize, sortField, sortOrder, filters, dateProperty, from, to));
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public int loadMaxCount(Map<String, Object> filters) {
    return getEsf().loadMaxCount(domainClass, filters);
  }

  @SuppressWarnings({"unchecked"})
  protected DOMAIN toDomain(DTO dto) {
    if (dto == null) return null;
    try {
      DOMAIN domain = domainClass.newInstance();
      if (ClassUtils.implementsInterface(domain, DomainEntity.class)) {
        ((DomainEntity) domain).fromDTO(dto);
      } else {
        PropertyUtils.copyProperties(domain, dto);
      }
      return domain;
    } catch (Exception ex) {
      log.error("Error in toDomain(). Domain class: " + domainClass.toString() + " : " + ex.getMessage());
      ex.printStackTrace();
      throw new GenericServiceException(ex.getMessage(), ex);
    }
  }

  protected List<DOMAIN> toDomain(List<DTO> dtoList) {
    if (dtoList == null) {
      return new ArrayList<>();
    }
    List<DOMAIN> result = new ArrayList<>(dtoList.size());
    for (DTO dto : dtoList) {
      result.add(toDomain(dto));
    }
    return result;
  }

  protected Set<DOMAIN> toDomain(Set<DTO> dtoSet) {
    if (dtoSet == null) {
      return new HashSet<>();
    }
    Set<DOMAIN> result = new HashSet<>(dtoSet.size());
    for (DTO dto : dtoSet) {
      result.add(toDomain(dto));
    }
    return result;
  }

  @SuppressWarnings({"unchecked"})
  protected DTO toDTO(DOMAIN domain) {
    if (domain == null) return null;
    try {
      if (ClassUtils.implementsInterface(domain, DomainEntity.class)) {
        DTO dto = ((DomainEntity<DTO>) domain).toDTO();
        postProcess(dto);
        return dto;
      } else {
        DTO dto = dtoClass.newInstance();
        PropertyUtils.copyProperties(dto, domain);
        postProcess(dto);
        return dto;
      }
    } catch (Exception ex) {
      log.error("Error in toDTO(). DTO class: " + dtoClass.toString() + " : " + ex.getMessage());
      ex.printStackTrace();
      throw new GenericServiceException(ex.getMessage(), ex);
    }
  }

  protected List<DTO> toDTO(List<DOMAIN> domainList) {
    if (domainList == null) {
      return new ArrayList<>();
    }
    List<DTO> result = new ArrayList<DTO>(domainList.size());
    for (DOMAIN entity : domainList) {
      result.add(toDTO(entity));
    }
    return result;
  }

  protected Set<DTO> toDTO(Set<DOMAIN> domainSet) {
    if (domainSet == null) {
      return new HashSet<>();
    }
    Set<DTO> result = new HashSet<>(domainSet.size());
    for (DOMAIN entity : domainSet) {
      result.add(toDTO(entity));
    }
    return result;
  }

  @SuppressWarnings({"unchecked"})
  protected Object toDomainViaMapping(Object dto) {
    try {
      Class domainClass = classMapping.get(dto.getClass());
      if (domainClass != null) {
        Object domain = domainClass.newInstance();
        if (ClassUtils.implementsInterface(domain, DomainEntity.class)) {
          DomainEntity d = (DomainEntity) domain;
          d.fromDTO(dto);
          return d;
        } else {
          log.error("Class does not implement DomainEntity: " + domain.getClass());
          return null;
        }
      } else {
        log.error("Unmapped dto class: " + dto);
        return null;
      }
    } catch (Exception e) {
      log.error("Error in toDomainViaMapping(): " + e.getMessage());
      e.printStackTrace();
    }
    return null;
  }

  protected void handleUpdate() {
    log.debug("handleUpdate()");
  }

  protected void postProcess(DTO dto) {
    log.debug("postProcess()");
  }

  public void addClassMapping(Class dto, Class domain) {
    classMapping.put(dto, domain);
  }

  public ExtendedSessionFactory getEsf() {
    return esf;
  }

  public void setEsf(ExtendedSessionFactory esf) {
    this.esf = esf;
  }

  public TransactionTemplate getTransactionTemplate() {
    return transactionTemplate;
  }

  public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
    this.transactionTemplate = transactionTemplate;
  }

}
