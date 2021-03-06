package com.apress.projpa2.chap9;

import java.util.*;
import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.apress.projpa2.ProJPAUtil;

import examples.model.Employee;
import examples.model.Project;

/**
 * Pro JPA 2 Chapter 9 Criteria API
 *
 * Listing 9-2. Employee Search Using Criteria API - Use join and distinct       p.229
 *
 * See also Predicates_9_3
 
    six possible clauses to be used in a select query:
    SELECT, FROM, WHERE, ORDER BY, GROUP BY and HAVING

    Table 9-1.  Criteria API Select Query Clause Methods           p.234
    JP QL Clause    Criteria API Interface  Method
    SELECT          CriteriaQuery           select()
                    Subquery                select()
    FROM            AbstractQuery           from()
    WHERE           AbstractQuery           where()
    ORDER BY        CriteriaQuery           orderBy()
    GROUP BY        AbstractQuery           groupBy()
    HAVING          AbstractQuery           having()
 *
 */
public class Predicates_9_2_join_distinct {

    EntityManager em;

    public Predicates_9_2_join_distinct() {
        String unitName = "jpqlExamples"; // = args[0];
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(unitName);
        em = emf.createEntityManager();
    }

    /**
     * Listing 9-2. Employee Search Using Criteria API    p.230
     *
     * Compare JPQLQuery#findEmployees
     */
    public List<Employee> findEmployees(String name, String deptName, String projectName, String city, boolean distinct) {

    	System.out.println("findEmployees(" + name + ", " + deptName + ", " + projectName + ", " + distinct + ")");

        // CriteriaBuilder is a factory with which we create the query definition itself, an instance of the CriteriaQuery
        // interface, as well as many of various components of the query definition such as conditional expressions
        // Almost all of the objects created by CriteriaBuilder methods are immutable
        CriteriaBuilder cb = em.getCriteriaBuilder();
        // It is worth noting that, despite the name, a CriteriaQuery instance is not a Query object that may be invoked to get
        // results from the database. It is a query definition that may be passed to the createQuery() method of the EntityManager
        // interface in place of a JP QL string.
        // A newly created CriteriaQuery object is basically an empty shell
        CriteriaQuery<Employee> c = cb.createQuery(Employee.class); // c - criteria query definition
        Root<Employee> emp = c.from(Employee.class); // A root in a criteria query corresponds to an identification variable in JP QL
                                                     // Calls to the from() method are additive. Each call adds another root to the query
        c.select(emp);
        c.distinct(distinct);
        Join<Employee,Project> project = emp.join("projects", JoinType.LEFT);

        // public interface Predicate extends Expression<java.lang.Boolean>
        // The type of a simple or compound predicate: a conjunction or disjunction of restrictions. A simple predicate
        // is considered to be a conjunction with a single conjunct.

        // public interface ParameterExpression<T> extends Parameter<T>, Expression<T> {}
        List<Predicate> criteria = new ArrayList<Predicate>();
        // Unlike JP QL where parameters are just an alias,
        // in the Criteria API parameters are strongly typed and created from the parameter() call
        // The ParameterExpression object that is returned can then be used in other parts of the query such as the WHERE or SELECT clauses
        if (name != null) {
            ParameterExpression<String> p = cb.parameter(String.class, "name");
            criteria.add(cb.equal(emp.get("name"), p));
        }
        if (deptName != null) {
            ParameterExpression<String> p = cb.parameter(String.class, "dept");
            criteria.add(cb.equal(emp.get("dept").get("name"), p));
        }
        if (projectName != null) {
            ParameterExpression<String> p = cb.parameter(String.class, "project");
            criteria.add(cb.equal(project.get("name"), p));
        }
        if (city != null) {
            ParameterExpression<String> p = cb.parameter(String.class, "city");
            criteria.add(cb.equal(emp.get("address").get("city"), p));
        }

        if (criteria.size() == 0) {
            throw new RuntimeException("no criteria");
        } else if (criteria.size() == 1) {
            c.where(criteria.get(0));
        } else {
            // notice odd invocation of the and() method. Unfortunately, the designers of the Collection.toArray() method decided that,
            // in order to avoid casting the return type, an array to be populated should also be passed in as an argument or 
            // an empty array in the case where we want the collection to create the array for us
            c.where(cb.and(criteria.toArray(new Predicate[0])));
        }

        TypedQuery<Employee> q = em.createQuery(c);
        if (name != null) { q.setParameter("name", name); }
        if (deptName != null) { q.setParameter("dept", deptName); }
        if (projectName != null) { q.setParameter("project", projectName); }
        if (city != null) { q.setParameter("city", city); }
        return q.getResultList();
    }

    public static void main(String[] args) throws Exception {
        Predicates_9_2_join_distinct test = new Predicates_9_2_join_distinct();
        String name = "John";
        String deptName = null;
        String projectName = null;
        String city = null;
        boolean distinct = true;
        ProJPAUtil.printResult(test.findEmployees(name, deptName, projectName, city, distinct));

        distinct = false;
        ProJPAUtil.printResult(test.findEmployees(name, deptName, projectName, city, distinct));

        name = "Stephanie";
        ProJPAUtil.printResult(test.findEmployees(name, deptName, projectName, city, distinct));

        name = "Marcus";
        ProJPAUtil.printResult(test.findEmployees(name, deptName, projectName, city, distinct));
    }
}