package com.apress.projpa2.chap9;

import java.util.*;
import java.util.logging.Logger;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import com.apress.projpa2.ProJPAUtil;

import examples.model.Employee;
import examples.model.Project;

/**
 * Pro JPA 2 Chapter 9 Criteria API
 * Listing 9-5. Employee Search Using Criteria API - sub query                  p.246

 
    mysql> use projpa2
    mysql> select * from EMPLOYEE;
    +----+-----------+--------+------------+------------+---------------+------------+
    | ID | NAME      | SALARY | STARTDATE  | ADDRESS_ID | DEPARTMENT_ID | MANAGER_ID |
    +----+-----------+--------+------------+------------+---------------+------------+
    |  1 | John      |  55000 | 2001-01-01 |          1 |             2 |          9 |
    |  2 | Rob       |  53000 | 2001-05-23 |          2 |             2 |          9 |
    |  3 | Peter     |  40000 | 2002-08-06 |          3 |             2 |          9 |
    |  4 | Frank     |  41000 | 2003-02-17 |          4 |             1 |         10 |
    |  5 | Scott     |  60000 | 2004-11-14 |          5 |             1 |         10 |
    |  6 | Sue       |  62000 | 2005-08-18 |          6 |             1 |         10 |
    |  7 | Stephanie |  54000 | 2006-06-07 |          7 |             1 |         10 |
    |  8 | Jennifer  |  45000 | 1999-08-11 |          8 |             1 |       NULL |
    |  9 | Sarah     |  52000 | 2002-04-26 |          9 |             2 |         10 |
    | 10 | Joan      |  59000 | 2003-04-16 |         10 |             1 |       NULL |
    | 11 | Marcus    |  35000 | 1995-07-22 |       NULL |          NULL |       NULL |
    | 12 | Joe       |  36000 | 1995-07-22 |       NULL |             3 |         11 |
    | 13 | Jack      |  43000 | 1995-07-22 |       NULL |             3 |       NULL |
    +----+-----------+--------+------------+------------+---------------+------------+

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
public class Predicates_9_5_subquery {

    private static final Logger LOGGER = Logger.getLogger(Predicates_9_5_subquery.class.getName());

    EntityManager em;

    public Predicates_9_5_subquery() {
        String unitName = "jpqlExamples"; // = args[0];
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(unitName);
        em = emf.createEntityManager();
    }

    /**
     *
     * /projpa2/src/main/resources/examples/Chapter9/03-empSearchSubQuery/src/model/examples/stateless/SearchService.java
     *
     */
    public List<Employee> findEmployees_Subquery_p246(String name, String deptName, String projectName, String city) {
        LOGGER.info("findEmployees_Subquery_p246('" + name + "%', " + deptName + ", " + projectName + ", " + city + ")");
        System.out.println("findEmployees_Subquery_p246('" + name + "%', " + deptName + ", " + projectName + ", " + city + ")");

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Employee> c = cb.createQuery(Employee.class); // c - criteria query definition
        Root<Employee> emp = c.from(Employee.class); // A root in a criteria query corresponds to an identification variable in JP QL
                                                     // Calls to the from() method are additive. Each call adds another root to the query
        c.select(emp);
        //see Predicates_9_2_join_distinct#findEmployees
        //c.distinct(true); // distinct no longer need by using sub query
        //Join<Employee,Project> project = emp.join("projects", JoinType.LEFT);

        List<Predicate> criteria = new ArrayList<Predicate>();
        if (name != null) {
            ParameterExpression<String> pe1 = cb.parameter(String.class, "nameUpper");
            ParameterExpression<String> pe2 = cb.parameter(String.class, "nameLower");
            Predicate p1 = cb.like(emp.get("name"), pe1);
            Predicate p2 = cb.like(emp.get("name"), pe2);
            criteria.add(cb.or(p1, p2));
        }
        if (deptName != null) {
            ParameterExpression<String> p = cb.parameter(String.class, "dept");
            criteria.add(cb.equal(emp.get("dept").get("name"), p));
        }
        if (projectName != null) {
            /*  SELECT e
                FROM Employee e
                WHERE e IN (SELECT emp FROM Project p JOIN p.employees emp WHERE p.name = :project)
             */
//            Subquery<Employee> sq = c.subquery(Employee.class); // from p.246 failed
//            Root<Project> project = sq.from(Project.class);
//            Join<Project,Employee> sqEmp = project.join("employees");
//            sq.select(sqEmp)
//              .where(cb.equal(project.get("name"), cb.parameter(String.class, "project")));
//            criteria.add(cb.in(emp).value(sq));
            // from Chapter9/03-empSearchSubQuery/src/model/examples/stateless/SearchService.java lines 85-91
            Subquery<Integer> sq = c.subquery(Integer.class);
            Root<Project> project = sq.from(Project.class);
            Join<Project,Employee> sqEmp = project.join("employees");
            sq.select(sqEmp.<Integer>get("id"))
              .where(cb.equal(project.get("name"), cb.parameter(String.class, "project")));
            criteria.add(cb.in(emp.get("id")).value(sq));
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
        if (name != null) { 
            q.setParameter("nameUpper", name.toUpperCase() + "%");
            q.setParameter("nameLower", name.toLowerCase() + "%");
        }
        if (projectName != null) { 
            q.setParameter("project", projectName);
        }
        return q.getResultList();
    }

    /**
     * /projpa2/src/main/resources/examples/Chapter9/06-canonicalMetamodelQuery/src/model/examples/stateless/SearchService.java
     */
    public List<Employee> findEmployees_Subquery_p247(String name, String projectName, boolean distinct, boolean subqueryIn) {
        System.out.println("findEmployees_Subquery_p247('" + name + "%', " + projectName + ", distinct: " + distinct + ", subqueryIn: " + subqueryIn + ")");

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Employee> c = cb.createQuery(Employee.class); // c - criteria query definition
        Root<Employee> emp = c.from(Employee.class); // A root in a criteria query corresponds to an identification variable in JP QL
                                                     // Calls to the from() method are additive. Each call adds another root to the query
        c.select(emp);
        //c.distinct(true); // distinct no longer need by using sub query

        List<Predicate> criteria = new ArrayList<Predicate>();
        if (name != null) {
            ParameterExpression<String> pe1 = cb.parameter(String.class, "nameUpper");
            ParameterExpression<String> pe2 = cb.parameter(String.class, "nameLower");
            Predicate p1 = cb.like(emp.get("name"), pe1);
            Predicate p2 = cb.like(emp.get("name"), pe2);
            criteria.add(cb.or(p1, p2));
        }
        if (projectName != null) {
            // p.247 use EXISTS instead of IN and shift the conditional expression into the WHERE clause of the subquery
            /*  SELECT e FROM Employee e
                WHERE EXISTS (SELECT p FROM Project p JOIN p.employees emp WHERE emp = e AND p.name = :name)
            */
            Subquery<Project> sq = c.subquery(Project.class);
            Root<Project> project = sq.from(Project.class);
            Join<Project,Employee> sqEmp = project.join("employees");
            sq.distinct(distinct);
            if (subqueryIn) {
                sq.select(project)
                  .where(cb.equal(sqEmp, emp),
                         cb.in(project.get("name")).value(cb.parameter(String.class,"project")));
            } else {
                sq.select(project)
                  .where(cb.equal(sqEmp, emp),
                         cb.equal(project.get("name"), cb.parameter(String.class,"project")));
            }
            criteria.add(cb.exists(sq));
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
        if (name != null) { 
            q.setParameter("nameUpper", name.toUpperCase() + "%");
            q.setParameter("nameLower", name.toLowerCase() + "%");
        }
        if (projectName != null) { 
            q.setParameter("project", projectName);
        }
        return q.getResultList();
    }

    public List<Employee> findEmployees_Subquery_p248(String name, String projectName) {
        System.out.println("findEmployees_Subquery_p247('" + name + "%', " + projectName + ")");

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Employee> c = cb.createQuery(Employee.class); // c - criteria query definition
        Root<Employee> emp = c.from(Employee.class); // A root in a criteria query corresponds to an identification variable in JP QL
                                                     // Calls to the from() method are additive. Each call adds another root to the query
        c.select(emp);
        //c.distinct(true); // distinct no longer need by using sub query

        List<Predicate> criteria = new ArrayList<Predicate>();
        if (name != null) {
            ParameterExpression<String> pe1 = cb.parameter(String.class, "nameUpper");
            ParameterExpression<String> pe2 = cb.parameter(String.class, "nameLower");
            Predicate p1 = cb.like(emp.get("name"), pe1);
            Predicate p2 = cb.like(emp.get("name"), pe2);
            criteria.add(cb.or(p1, p2));
        }
        if (projectName != null) {
            /*
             * Root object obtained by from() method only accepts a persistent class type
             * Use correlate in sub query
             *
             * SELECT e FROM Employee e WHERE EXISTS 
             *      (SELECT p FROM e.projects p WHERE p.name = :name)
             */
            Subquery<Project> sq = c.subquery(Project.class);
            Root<Employee> sqEmp = sq.correlate(emp); // replace Root<Project> project = sq.from(Project.class);
            Join<Employee,Project> project = sqEmp.join("projects");
            sq.select(project)
              .where(cb.equal(project.get("name"), cb.parameter(String.class,"project")));
            criteria.add(cb.exists(sq));
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
        if (name != null) { 
            q.setParameter("nameUpper", name.toUpperCase() + "%");
            q.setParameter("nameLower", name.toLowerCase() + "%");
        }
        if (projectName != null) { 
            q.setParameter("project", projectName);
        }
        return q.getResultList();
    }

    /**
     *
        select * from PROJECT;
        +----+-------+--------------------+
        | ID | DTYPE | NAME               |
        +----+-------+--------------------+
        |  1 | DP    | Design Release2    |
        |  2 | DP    | Release1           |
        |  3 | QP    | Test Release2      |
        |  4 | P     | Implement Release3 |
        +----+-------+--------------------+
     */
    public static void main(String[] args) throws Exception {
        Predicates_9_5_subquery test = new Predicates_9_5_subquery();
        String name = "Jo";
        ProJPAUtil.printResult(test.findEmployees_Subquery_p246(name, null, "Design Release2", null));

        //retList = test.findEmployees_Subquery_p247(name, "Design Release2, Release1", true); // 0 rows
        ProJPAUtil.printResult(test.findEmployees_Subquery_p247(name, "Design Release2", true, true));

        ProJPAUtil.printResult(test.findEmployees_Subquery_p247(name, "Design Release2", false, true));

        ProJPAUtil.printResult(test.findEmployees_Subquery_p247(name, "Design Release2", true, false));

        ProJPAUtil.printResult(test.findEmployees_Subquery_p247(name, "Design Release2", false, false));

        ProJPAUtil.printResult(test.findEmployees_Subquery_p248(name, "Design Release2"));
    }
}