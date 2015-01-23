package net.borkert.util;

import net.borkert.util.DateTool;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import java.io.Serializable;
import java.util.*;

public abstract class GenericLazyLoadingBean<DTO extends Serializable, ID extends Serializable>
    extends GenericBeanBase<DTO, ID> {

  private static Logger log = LogManager.getLogger(GenericLazyLoadingBean.class);

  protected LazyDataModel<DTO> lazyModel;
  private Date limitFrom;
  private Date limitTo;
  private String dateProperty;

  public LazyDataModel<DTO> getLazyModel() {
    if (lazyModel == null) {
      lazyModel = new LazyDataModel<DTO>() {
        @Override
        public List<DTO> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
          log.debug("load(): Loading data between " + first + " and " + (first + pageSize) + ". Order by " + sortField + " " + sortOrder);
          if (dateProperty != null) {
            log.debug("load(): Limit from " + limitFrom + " to " + limitTo);
          }
          log.debug("load(): RowCount is: " + lazyModel.getRowCount());
          List<DTO> result = new ArrayList<DTO>();
          try {
            if (sortField == null) {
              sortField = "id";
            }
            if (getDateProperty() != null) {
              result = getService().load(first, pageSize, sortField, sortOrder != SortOrder.DESCENDING, filters, getDateProperty(), getLimitFrom(), DateTool.eos(getLimitTo()));
            } else {
              result = getService().load(first, pageSize, sortField, sortOrder != SortOrder.DESCENDING, filters);
            }
            lazyModel.setRowCount(getService().loadMaxCount(new HashMap<>()));
          } catch (Exception e) {
            log.error("load(): Error: " + e.getMessage());
            e.printStackTrace();
          }
          log.debug("load(): Returning " + result.size() + " " + dtoClass);
          return result;
        }

        public Object getRowKey(DTO dto) {
          try {
            log.debug("getRowKey(" + dto.toString() + ") returning " + PropertyUtils.getProperty(dto, "id"));
            return PropertyUtils.getProperty(dto, "id");
          } catch (Exception e) {
            log.error("No property ID: " + dto.getClass());
          }
          return dto.hashCode();
        }

        public DTO getRowData(String rowKey) {
          log.debug("getRowData(" + rowKey + ")");
          return getService().findOneByProperty("id", Long.parseLong(rowKey));
        }
      };
    }
    return lazyModel;
  }

  public Date getLimitFrom() {
    return limitFrom;
  }

  public void setLimitFrom(Date limitFrom) {
    this.limitFrom = limitFrom;
  }

  public Date getLimitTo() {
    return limitTo;
  }

  public void setLimitTo(Date limitTo) {
    this.limitTo = limitTo;
  }

  public String getDateProperty() {
    return dateProperty;
  }

  public void setDateProperty(String dateProperty) {
    this.dateProperty = dateProperty;
  }
}
