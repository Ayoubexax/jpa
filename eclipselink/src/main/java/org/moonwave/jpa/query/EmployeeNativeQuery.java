package org.moonwave.jpa.query;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.moonwave.jpa.model.pojo.Employee;

public class EmployeeNativeQuery 
{
	
	private EntityManager em;
	private EntityManagerFactory emf;
	
	public void setUp() throws Exception{
		emf=Persistence.createEntityManagerFactory("jpa-mysql");
		em=emf.createEntityManager();
	}
	
	public void tearDown()throws Exception{
		em.close();
		emf.close();
	}

	@SuppressWarnings("unchecked")
	public void query(){
		Query query = em.createNativeQuery("select e.emp_no, e.first_name as firstName, e.last_name as lastName," + 
				"t.title from employees e join titles t on e.emp_no = t.emp_no", Employee.class);
		query.setMaxResults(30);
		List<Employee> list = (List<Employee>) query.getResultList();
		int i = 0;
		for (Employee emp : list) {
			System.out.println(++i + ": " + emp.toString());
		}
	}
	
    public static void main( String[] args )
    {
    	EmployeeNativeQuery test = new EmployeeNativeQuery();
    	try {
    		test.setUp();
    		test.query();
    		test.tearDown();
    	} catch (Exception e) {
    		System.out.println(e);
    	}
    }
}
