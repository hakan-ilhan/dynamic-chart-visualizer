# Dinamik Chart Görselleştirme Uygulaması (Dynamic Chart Visualizer)

Bu proje, kullanıcının veritabanı bağlantı bilgilerini girerek, mevcut veri objelerini (View/Fonksiyon) seçmesini ve bu verileri çeşitli grafik türlerinde (Bar, Line, Radar) dinamik olarak görselleştirmesini sağlayan bir uygulamadır. 

## Proje Mimarisi ve Teknolojiler

Bu proje, katmanlı bir mimariye sahiptir ve aşağıdaki ana teknolojileri kullanır:

### Frontend (Client-Side)
* **React:** Kullanıcı arayüzünü oluşturmak için modern JavaScript kütüphanesi.
* **Axios:** Backend API ile HTTP istekleri yapmak için Promise tabanlı HTTP istemcisi.
* **Chart.js:** Dinamik ve etkileşimli grafikler oluşturmak için esnek JavaScript grafik kütüphanesi.
* **CSS (Vanilla/Modüller):** Basit ve temiz bir kullanıcı arayüzü tasarımı için.

### Backend (Server-Side)
* **Java 17:** API'nin temel programlama dili.
* **Spring Boot:** RESTful API'ler geliştirmek için tercih edilen framework.
* **Spring Data JDBC:** Veritabanı etkileşimleri için.
* **PostgreSQL JDBC Driver:** PostgreSQL veritabanına bağlanmak için.

### Veritabanı
* **PostgreSQL:** Veri depolama ve yönetim sistemi olarak kullanılmıştır.

## Kurulum Adımları

Projeyi yerel ortamınızda çalıştırmak için aşağıdaki adımları takip edin:

### 1. Veritabanı Kurulumu ve Yapılandırması

* PostgreSQL sunucunuzun çalıştığından emin olun.
* `chart_visualizer_db` adında yeni bir veritabanı oluşturun (veya backend uygulamasının `application.properties` dosyasında yapılandırılan veritabanı adını kullanın).
* Veritabanı kullanıcınızın (varsayılan: `postgres`) ve şifrenizin  doğru olduğundan emin olun ve bu kullanıcıya `chart_visualizer_db` üzerinde gerekli izinleri verin.
* **Örnek Veritabanı ve Veri Objeleri:**
    * Aşağıdaki SQL script'ini kullanarak örnek tabloları, view'leri ve fonksiyonları oluşturun:
        ```sql
        -- Örnek Tablo (products)
        CREATE TABLE products (
            product_id SERIAL PRIMARY KEY,
            product_name VARCHAR(255) NOT NULL,
            price NUMERIC(10, 2) NOT NULL,
            stock_quantity INTEGER NOT NULL
        );
	
	Örnek Tablo (customers)
	CREATE TABLE customers (
            customer_id SERIAL PRIMARY KEY,
            customer_name VARCHAR(255) NOT NULL,
            city VARCHAR(100),
            registration_date DATE
        );
        INSERT INTO customers (customer_name, city, registration_date) VALUES
        ('Ayşe Yılmaz', 'İstanbul', '2023-01-01'),
        ('Can Demir', 'Ankara', '2023-03-10'),
        ('Elif Kaya', 'İstanbul', '2023-05-22'),
        ('Murat Yıldız', 'İzmir', '2023-07-01');
        ```

        -- Örnek Tablo (orders)
        CREATE TABLE orders (
            order_id SERIAL PRIMARY KEY,
            customer_name VARCHAR(255) NOT NULL,
            order_date DATE NOT NULL,
            total_amount NUMERIC(10, 2) NOT NULL
        );

        -- Örnek Tablo (order_details)
        CREATE TABLE order_details (
            detail_id SERIAL PRIMARY KEY,
            order_id INTEGER REFERENCES orders(order_id),
            product_id INTEGER REFERENCES products(product_id),
            quantity INTEGER NOT NULL,
            price_at_order NUMERIC(10, 2) NOT NULL
        );

        -- Örnek Veri Ekleme
        INSERT INTO products (product_name, price, stock_quantity) VALUES
        ('Laptop', 1200.00, 50),
        ('Mouse', 25.00, 200),
        ('Keyboard', 75.00, 100);

        INSERT INTO orders (customer_name, order_date, total_amount) VALUES
        ('Alice Smith', '2023-01-15', 1250.00),
        ('Bob Johnson', '2023-02-20', 100.00),
        ('Alice Smith', '2023-02-28', 75.00);

        INSERT INTO order_details (order_id, product_id, quantity, price_at_order) VALUES
        (1, 1, 1, 1200.00),
        (1, 2, 2, 25.00),
        (2, 3, 1, 75.00),
        (3, 2, 3, 25.00);

        -- Örnek View: customer_order_details_view
        CREATE OR REPLACE VIEW customer_order_details_view AS
        SELECT
            o.order_id,
            o.customer_name,
            o.order_date,
            od.quantity,
            od.price_at_order,
            p.product_name
        FROM
            orders o
        JOIN
            order_details od ON o.order_id = od.order_id
        JOIN
            products p ON od.product_id = p.product_id;

        -- Örnek Fonksiyon: get_monthly_sales_summary
        CREATE OR REPLACE FUNCTION get_monthly_sales_summary()
        RETURNS TABLE (
            sale_year INTEGER,
            sale_month INTEGER,
            month_name TEXT,
            total_sales NUMERIC
        )
        LANGUAGE plpgsql
        AS $$
        BEGIN
            RETURN QUERY
            SELECT
                EXTRACT(YEAR FROM order_date)::INTEGER AS sale_year,
                EXTRACT(MONTH FROM order_date)::INTEGER AS sale_month,
                TO_CHAR(order_date, 'Month') AS month_name,
                SUM(total_amount) AS total_sales
            FROM
                orders
            GROUP BY
                sale_year, sale_month, month_name
            ORDER BY
                sale_year, sale_month;
        END;
        $$;

        
        CREATE OR REPLACE FUNCTION calculate_order_tax(p_order_id INTEGER, p_tax_rate NUMERIC)
        RETURNS NUMERIC
        LANGUAGE plpgsql
        AS $$
        DECLARE
            v_total_amount NUMERIC;
            v_tax_amount NUMERIC;
        BEGIN
            SELECT total_amount INTO v_total_amount FROM orders WHERE order_id = p_order_id;
            v_tax_amount := v_total_amount * p_tax_rate;
            RETURN v_tax_amount;
        END;
        $$;
        
        -- Parametreli Fonksiyon Örneği (birden fazla müşteri dönen)
        CREATE OR REPLACE FUNCTION get_customers_by_city(p_city VARCHAR)
        RETURNS TABLE(
            customer_id INTEGER,
            customer_name VARCHAR,
            city VARCHAR,
            registration_date DATE
        )
        LANGUAGE plpgsql
        AS $$
        BEGIN
            RETURN QUERY
            SELECT c.customer_id, c.customer_name, c.city, c.registration_date
            FROM customers c
            WHERE c.city = p_city;
        END;
        $$;

       
        

### 2. Backend Uygulamasını Çalıştırma

1.  `backend/` klasörüne gidin: `cd chart-visualizer-backend`
2.  Maven kullanarak projeyi derleyin ve çalıştırın:
    * Maven için: `./mvnw spring-boot:run` (Windows'ta `mvnw.cmd spring-boot:run`)
    * Alternatif olarak, tercih ettiğiniz IDE (IntelliJ IDEA, Eclipse) üzerinden `main` sınıfını çalıştırabilirsiniz.
3.  Uygulamanın `http://localhost:8080` üzerinde çalıştığından emin olun.

### 3. Frontend Uygulamasını Çalıştırma

1.  `frontend/` klasörüne gidin: `cd chart-visualizer-frontend`
2.  Gerekli bağımlılıkları yükleyin: `npm install`
3.  Uygulamayı başlatın: `npm start`
4.  Tarayıcınız otomatik olarak `http://localhost:3000` adresinde açılacaktır.

## Kullanım

1.  Uygulama açıldığında, **"Veritabanı Bağlantı Bilgileri"** bölümüne PostgreSQL bağlantı detaylarınızı girin. (Varsayılanlar genellikle yerel kurulum için geçerlidir: Host: `localhost`, Veritabanı Adı: `chart_visualizer_db`, Kullanıcı Adı: `postgres`, Parola: `*****` - **kendi parolanızı kontrol edin**).
2.  **"Objeleri Getir ve Bağlan"** düğmesine tıklayın. Başarılı olursa, "Veri Objesi Seç" açılır listesi veritabanınızdaki uygun view'ler ve fonksiyonlarla dolacaktır.
3.  **"Veri Objesi Seç"** kısmından bir obje (örn. `customer_order_details_view` veya `get_monthly_sales_summary`) seçin.
4.  Eğer seçilen objenin parametreleri varsa (örn. `get_customers_by_city`), **"Parametreler"** bölümünde ilgili giriş alanları belirecektir. Gerekli değerleri girin.
5.  **"Grafik Verilerini Çek"** düğmesine tıklayın. Veriler başarıyla çekildikten sonra, "Veri Haritalama" bölümü görünür olacaktır.
6.  **"X Ekseni"** ve **"Y Ekseni"** açılır listelerinden uygun sütunları seçin. (Y ekseni için sadece sayısal sütunlar listelenir.)
7.  **"Grafik Tipi"** bölümünden istediğiniz grafik türünü (Bar, Line, Radar) seçin.
8.  Seçimler tamamlandığında, dinamik grafik ekranın alt kısmında belirecektir.






