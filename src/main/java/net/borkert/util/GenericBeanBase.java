package net.borkert.util;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.util.HashSet;
import java.util.Set;

public abstract class GenericBeanBase<DTO extends Serializable, ID extends Serializable> {

  private static Logger log = LogManager.getLogger(GenericBeanBase.class);

  protected final Class<DTO> dtoClass;
  protected final Class<ID> idClass;
  protected DTO selected;
  protected DTO[] selectedMultiple;
  protected GenericService<DTO, ID> service;
  protected final Set remove = new HashSet();

  @SuppressWarnings({"unchecked"})
  public GenericBeanBase() {
    this.dtoClass = (Class<DTO>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    this.idClass = (Class<ID>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    DTO dto = createEntity();
    if (!PropertyUtils.isReadable(dto, "id")) {
      throw new GenericBeanException("Class " + dtoClass.toString() + " has no id property");
    }
    try {
      Class c = PropertyUtils.getPropertyType(dto, "id");
      if (c == long.class) {
        c = Long.class;
      }
      if (c == int.class) {
        c = Integer.class;
      }
      if (c != idClass) {
        throw new Error("Invalid ID class: " + PropertyUtils.getPropertyType(dto, "id") + " != " + idClass);
      }
    } catch (Exception e) {
      throw new GenericBeanException(e.getMessage(), e);
    }
  }

  public void clearSelection() {
    setSelected(createEntity());
    setSelectedMultiple(null);
  }

  public void save() {
    if (getSelected() != null) {
      getService().addOrUpdate(getSelected(), getRemove());
      getRemove().clear();
      clearSelection();
    } else {
      log.warn("save(): nothing selected");
    }
  }

  public void delete() {
    if (getSelected() != null) {
      getService().delete(getSelectedId());
      clearSelection();
    }
  }

  @SuppressWarnings({"unchecked"})
  public void deleteMultiple() {
    if (getSelectedMultiple() != null && getSelectedMultiple().length > 0) {
      ID[] ids = (ID[]) Array.newInstance(idClass, getSelectedMultiple().length);
      for (int i = 0; i < getSelectedMultiple().length; i++) {
        ids[i] = getIdFromEntity(getSelectedMultiple()[i]);
      }
      getService().delete(ids);
      clearSelection();
    }
  }

  @SuppressWarnings({"unchecked"})
  public ID[] getSelectedMultipleIds() {
    ID[] ids = (ID[]) Array.newInstance(idClass, getSelectedMultiple().length);
    for (int i = 0; i < getSelectedMultiple().length; i++) {
      ids[i] = getIdFromEntity(getSelectedMultiple()[i]);
    }
    return ids;
  }

  public void message(String head, String message) {
    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(head, message));
  }

  protected DTO createEntity() {
    try {
      return dtoClass.newInstance();
    } catch (Exception e) {
      throw new GenericBeanException("Exception in createEntity(): " + e.getMessage(), e);
    }
  }

  @SuppressWarnings({"unchecked"})
  public ID getSelectedId() {
    if (getSelected() == null) {
      return null;
    }
    try {
      return (ID) PropertyUtils.getProperty(getSelected(), "id");
    } catch (Exception e) {
      throw new GenericBeanException("Exception in getSelectedId(): " + e.getMessage(), e);
    }
  }

  @SuppressWarnings({"unchecked"})
  public ID getIdFromEntity(DTO dto) {
    if (dto == null) {
      return null;
    }
    try {
      return (ID) PropertyUtils.getProperty(dto, "id");
    } catch (Exception e) {
      throw new GenericBeanException("Exception in getSelectedId(): " + e.getMessage(), e);
    }
  }

  @SuppressWarnings({"unchecked"})
  public void remove(Object entity) {
    getRemove().add(entity);
  }

  public GenericService<DTO, ID> getService() {
    return service;
  }

  public void setService(GenericService<DTO, ID> service) {
    this.service = service;
  }

  public DTO getSelected() {
    return selected != null ? selected : createEntity();
  }

  public void setSelected(DTO selected) {
    this.selected = selected;
  }

  public Set getRemove() {
    return remove;
  }

  public DTO[] getSelectedMultiple() {
    return selectedMultiple;
  }

  public void setSelectedMultiple(DTO[] selectedMultiple) {
    this.selectedMultiple = selectedMultiple;
  }
}
