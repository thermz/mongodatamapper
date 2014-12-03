/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.thermz.minilib.mongodatamapper;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import static java.util.stream.Collectors.toList;

/**
 *
 * @author RMuzzi
 */
public class MongoDataMapper {
	public static <T> List<T> mongoUnmarshall(final List<DBObject> dbList, final Class<T> clazz){
		return dbList.stream()
			.map( o -> mongoUnmarshall(o, clazz) )
			.collect( toList() );
	}
	
	public static <T> List<T> mongoUnmarshall(final BasicDBList dbList, final Class<T> clazz){
		return dbList.stream()
			.map( o -> mongoUnmarshall((DBObject)o, clazz) )
			.collect( toList() );
	}
	
	public static <T> T mongoUnmarshall(final DBObject dbo, final Class<T> clazz){
		return unchecked( ()-> {
			T pojo = instanceEmpty(clazz);
			List<Field> fields = getFields(clazz);
			for (Field field : fields) {
				JsonProperty annotation = field.getAnnotation(JsonProperty.class);
				String valueToSetFrom = annotation != null? annotation.value() : field.getName();
				field.setAccessible(true);
				Object dboField = dbo.get(valueToSetFrom);
				try{
					boolean isDBO = (dboField instanceof DBObject);
					boolean isDBL = (dboField instanceof BasicDBList);
					if(isDBL){
						List<String> strings = ((BasicDBList)dboField).stream().map(i->(String)i).collect(toList());
						field.set(pojo, new ArrayList<>(strings)); //FIXME TODO works only with JSON String array
					}else if(isDBO){
						field.set(pojo, mongoUnmarshall((DBObject)dboField, field.getType()));
					}else{
						field.set(pojo, dboField);
					}
				} catch(Exception e){
					if(field.getType().isAssignableFrom(Date.class)) {
						if(dboField instanceof String){
							field.set(pojo, getUTCDate( (String)dboField ) );
						} else if( dboField instanceof BasicDBList ) {
							continue; //FIXME fix through logstash, why some timestamps are stored as DBList?
						}
					} else {
						//Field type incompatible with DBO type, try String instantiation
						String valueString = dbo.get(valueToSetFrom).toString();
						if(valueString!=null)
							field.set(pojo, instanceByString(field.getType(), valueString));
					}
				}
			}
			return pojo;
		});
	}
	
	public static DBObject mongoMarshall(final Object o){
		return unchecked(()-> {
			DBObject dbo = new BasicDBObject();
			List<Field> fields = new ArrayList<>(getFields(o.getClass()));
			Optional.ofNullable(o.getClass().getSuperclass()).ifPresent(s -> fields.addAll(getFields(s)));
			for (Field field : fields) {
				field.setAccessible(true);
				Object fieldContent = field.get(o);
				if(fieldContent==null) continue;
				if(field.isAnnotationPresent(MongoDBO.class))
					dbo.put(field.getName(), mongoMarshall(fieldContent));
				else
					dbo.put(field.getName(), field.getType().isEnum() ? fieldContent.toString() : fieldContent);
			}
			return dbo;
		}, new BasicDBObject());
	}
}
