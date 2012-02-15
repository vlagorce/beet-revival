package com.mtgi.analytics.jmx;

import static com.mtgi.jmx.export.naming.AppendNamingStrategy.quote;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.jmx.export.naming.ObjectNamingStrategy;

import com.mtgi.analytics.BehaviorEvent;
import com.mtgi.analytics.aop.BehaviorTrackingAdvice;

/**
 * A naming strategy for {@link BehaviorEvent} instances corresponding to Java method invocations,
 * as generated by {@link BehaviorTrackingAdvice}.  The returned {@link ObjectName} has form
 * <pre>application:type=method-monitor,package=java.package.name,group=sub.package.name,class=JavaClassName,name=methodName</pre>
 * The division between <code>package</code> and <code>group</code> is determined by the {@link #setPackageDepth(int)}
 * property, which has a default value of 2.  So, for example
 * <blockquote>
 * <dl>
 * <dt>com.mtgi.test.Type.someMethod()</dt>
 * <dd>package=com.mtgi,group=test,class=Type,name=someMethod</dd>
 * <dt>com.mtgi.Type.someMethod()</dt>
 * <dd>package=com.mtgi,class=Type,name=someMethod</dd>
 * <dt>Type.someMethod()</dt>
 * <dd>package=[default],class=Type,name=someMethod</dd>
 * </dl>
 * </blockquote>
 */
public class MethodNamingStrategy implements ObjectNamingStrategy {

	private int packageDepth = 2;
	
	public void setPackageDepth(int packageDepth) {
		this.packageDepth = packageDepth;
	}

	public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {

		BehaviorEvent event = (BehaviorEvent)managedBean;

		StringBuffer name = new StringBuffer(quote(event.getApplication()));
		name.append(":type=").append(quote(event.getType() + "-monitor"));
		
		String qname = event.getName();
		int methodDot = qname.lastIndexOf('.'); //dot separating method name from type
		if (methodDot < 0)
			throw new IllegalArgumentException(qname + " is not a fully-qualified Java method name");
		int typeDot = qname.lastIndexOf('.', methodDot - 1); //dot separating type name from package
		
		if (typeDot > 0) {
			int packageEnd = typeDot;
			if (packageDepth > 0) {
				//divide package namespace into package,group keys
				//according to the depth configured by "packageDepth"
				for (int d = packageDepth, dot = -1; d > 0 && dot < typeDot; --d)
					packageEnd = dot = qname.indexOf('.', dot + 1);
			}
			name.append(",package=").append(qname.substring(0, packageEnd));
			if (packageEnd < typeDot)
				name.append(",group=").append(qname.substring(packageEnd + 1, typeDot));
		} else {
			name.append(",package=[default]");
		}
		
		name.append(",class=").append(qname.subSequence(typeDot + 1, methodDot));
		name.append(",name=").append(qname.substring(methodDot + 1));
		
		return ObjectName.getInstance(name.toString());
	}

}
