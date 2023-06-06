package a;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

public class HibernateFactory {
    private static SessionFactory sessionFactory;

    private HibernateFactory() {
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Configuration configuration = new Configuration();
                configuration.setProperty("hibernate.show_sql", "true");
                configuration.setProperty("hibernate.format_sql", "true");
                configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQL95Dialect");
                configuration.setProperty("hibernate.connection.url", "jdbc:postgresql://localhost:5432/ArrowGame");
                configuration.setProperty("hibernate.connection.username", "postgres");
                configuration.setProperty("hibernate.connection.password", "0000");
                configuration.setProperty("hibernate.connection.autocommit", "true");
                configuration.setProperty("hibernate.hbm2ddl.auto", "validate");
                configuration.addAnnotatedClass(Leaders.class);
                StandardServiceRegistryBuilder builder = (new StandardServiceRegistryBuilder())
                        .applySettings(configuration.getProperties());
                sessionFactory = configuration.buildSessionFactory(builder.build());
            } catch (Exception var2) {
                System.out.println("Исключение" + String.valueOf(var2));
            }
        }

        return sessionFactory;
    }
}
