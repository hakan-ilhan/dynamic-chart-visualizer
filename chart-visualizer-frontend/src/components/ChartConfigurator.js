
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Line, Bar, Radar } from 'react-chartjs-2'; // Grafik türleri
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    BarElement,
    RadialLinearScale, 
    Filler, 
    Tooltip,
    Legend,
    Title 
} from 'chart.js';

// Chart.js'ye gerekli bileşenleri kaydet

ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    BarElement,
    RadialLinearScale,
    Filler,
    Tooltip,
    Legend,
    Title 
);

const API_BASE_URL = 'http://localhost:8080/api/charts'; // Backend API'mizin adresi

// Sayısal tipleri belirlemek için yardımcı fonksiyon
const isNumericType = (type) => {
    if (!type) return false;
    const lowerCaseType = type.toLowerCase();
    return (
        lowerCaseType.includes('int') ||
        lowerCaseType.includes('serial') || // SERIAL de int'e karşılık gelir
        lowerCaseType.includes('numeric') ||
        lowerCaseType.includes('decimal') ||
        lowerCaseType.includes('float') ||
        lowerCaseType.includes('double') ||
        lowerCaseType.includes('real')
    );
};

// Metin formatlama fonksiyonu (örn: "p_city_name" -> "P. City Name" veya "get_customers_by_city" -> "Get Customers By City")
const formatLabel = (text) => {
    if (!text) return '';
    return text
        .split('_') 
        .map(word => word.charAt(0).toUpperCase() + word.slice(1)) 
        .join(' '); 
};


function ChartConfigurator() {
   
    const [dbConfig, setDbConfig] = useState({ // Veritabanı bağlantı bilgileri
        host: 'localhost',
        dbName: 'chart_visualizer_db',
        user: 'postgres',
        password: '', // Veritabanı şifresi
    });

    const [dbObjects, setDbObjects] = useState([]); // Veritabanındaki View/Function isimleri
    const [selectedObject, setSelectedObject] = useState(''); // Seçilen veri objesi
    const [objectParameters, setObjectParameters] = useState([]); // Seçilen objenin beklediği parametreler
    const [paramValues, setParamValues] = useState({}); // Parametreler için kullanıcıdan alınan değerler

    const [chartDataRaw, setChartDataRaw] = useState(null); // Backend'den gelen ham veri (data ve columns içerir)
    const [availableColumns, setAvailableColumns] = useState([]); // Ham veriden çıkan sütunlar (metadata)
    
    // X ve Y eksenleri artık formatlanmış hallerini tutmuyor, direkt DB'den gelen adı tutuyor
    const [xAxisColumn, setXAxisColumn] = useState(''); // X ekseni için seçilen sütun adı
    const [yAxisColumn, setYAxisColumn] = useState(''); // Y ekseni için seçilen sütun adı
    const [chartType, setChartType] = useState('bar'); // Seçilen grafik türü (varsayılan: bar)

    const [loading, setLoading] = useState(false); 
    const [error, setError] = useState(''); 
    const [message, setMessage] = useState(''); 

    // --- Fonksiyonlar ---

    // Veritabanı bağlantı ayarlarını değiştirme
    const handleDbConfigChange = (e) => {
        setDbConfig({ ...dbConfig, [e.target.name]: e.target.value });
    };

    // Veritabanı objelerini (View/Function) çekme
    const fetchDbObjects = async () => {
        setLoading(true);
        setError('');
        setMessage('');
        setDbObjects([]); // Önceki objeleri temizle
        setSelectedObject(''); // Seçili objeyi temizle
        setObjectParameters([]); // Parametreleri temizle
        setParamValues({}); // Parametre değerlerini temizle
        setChartDataRaw(null); // Grafik verisini temizle
        setAvailableColumns([]); // Sütunları temizle
        setXAxisColumn(''); // Eksenleri temizle
        setYAxisColumn(''); // Eksenleri temizle

        try {
            const response = await axios.post(`${API_BASE_URL}/objects`, dbConfig);
            setDbObjects(response.data);
            setMessage('Veritabanına başarıyla bağlanıldı ve objeler çekildi.');
            if (response.data.length > 0) {
                setSelectedObject(response.data[0]); // İlk objeyi varsayılan olarak seç
            }
        } catch (err) {
            console.error('Veritabanı objeleri çekilirken hata oluştu:', err);
            setError('Veritabanı objeleri çekilirken hata oluştu: ' + (err.response?.data?.[0] || err.message));
        } finally {
            setLoading(false);
        }
    };

    // Seçilen objenin (eğer fonksiyonsa) parametrelerini çekme
    useEffect(() => {
        const getObjectParameters = async () => {
            if (!selectedObject) {
                setObjectParameters([]);
                setParamValues({});
                return;
            }
            setLoading(true);
            setError('');
            setMessage('');
            try {
                const response = await axios.post(`${API_BASE_URL}/object-parameters`, { ...dbConfig, objectName: selectedObject });
                setObjectParameters(response.data);
                
                const initialParamValues = {};
                response.data.forEach(param => {
                    initialParamValues[param.name] = ''; 
                });
                setParamValues(initialParamValues);
                setMessage(`'${formatLabel(selectedObject)}' için parametreler yüklendi.`);

            } catch (err) {
                console.error('Objenin parametreleri çekilirken hata oluştu:', err);
                if (err.response?.status === 400 || err.response?.data?.[0]?.name === 'error') {
                    // 400 hatası veya backend'den 'error' mesajı gelirse parametreleri temizle (muhtemelen bir View'dır)
                    setObjectParameters([]);
                    setParamValues({});
                    setMessage(`Seçili obje '${formatLabel(selectedObject)}' için parametre bulunamadı veya bu bir View'dır.`);
                } else {
                    setError('Objenin parametreleri çekilirken hata oluştu: ' + (err.response?.data?.error || err.message));
                }
            } finally {
                setLoading(false);
            }
        };
        getObjectParameters();
    }, [selectedObject, dbConfig.host, dbConfig.dbName, dbConfig.user, dbConfig.password]); 

    // Parametre değerlerini değiştirme
    const handleParamValueChange = (paramName, value) => {
        setParamValues(prevValues => ({
            ...prevValues,
            [paramName]: value
        }));
    };

    // Grafik verilerini çekme
    const fetchChartData = async () => {
        setLoading(true);
        setError('');
        setMessage('');
        setChartDataRaw(null); 
        setAvailableColumns([]); 
        setXAxisColumn(''); 
        setYAxisColumn(''); 

        if (!selectedObject) {
            setError('Lütfen bir veri objesi seçin.');
            setLoading(false);
            return;
        }

        const parametersToSend = objectParameters.map(param => ({
            name: param.name,
            value: paramValues[param.name],
            type: param.type 
        }));

        try {
            const response = await axios.post(`${API_BASE_URL}/data`, {
                ...dbConfig,
                objectName: selectedObject,
                parameters: parametersToSend
            });

            const { columns, data } = response.data; 
            setAvailableColumns(columns);
            setChartDataRaw(data);

            if (!data || data.length === 0) {
                setMessage('Veri bulunamadı.');
                setLoading(false);
                return;
            }

            // Otomatik olarak ilk uygun X ve Y eksenlerini seçmeye çalış
            const numericColumns = columns.filter(col => isNumericType(col.type));
            const categoryOrTemporalColumns = columns.filter(col => !isNumericType(col.type)); 

            let autoXAxis = '';
            if (categoryOrTemporalColumns.length > 0) {
                autoXAxis = categoryOrTemporalColumns[0].name;
            } else if (numericColumns.length > 0) {
                autoXAxis = numericColumns[0].name;
            }
            setXAxisColumn(autoXAxis);


            let autoYAxis = '';
            if (numericColumns.length > 0) {
                autoYAxis = numericColumns[0].name;
            } else if (categoryOrTemporalColumns.length > 0) {
                // Eğer sayısal yoksa, ilk kategorik/tarihseli Y yapabiliriz ama bu genellikle bir uyarı gerektirir
                autoYAxis = categoryOrTemporalColumns[0].name;
                setError("Uyarı: Hiç sayısal sütun bulunamadı. İlk kategorik sütun varsayılan Y ekseni olarak ayarlandı, bu bir sorun yaratabilir.");
            }
            setYAxisColumn(autoYAxis);

            setMessage('Grafik verileri başarıyla çekildi.');

        } catch (err) {
            console.error('Grafik verileri çekilirken hata oluştu:', err);
            setError('Grafik verileri çekilirken hata oluştu: ' + (err.response?.data?.error || err.message));
        } finally {
            setLoading(false);
        }
    };

    // Dinamik grafik veri hazırlığı
    const prepareChartData = () => {
        if (!chartDataRaw || chartDataRaw.length === 0 || !xAxisColumn || !yAxisColumn) {
            return null;
        }

        // Seçilen Y ekseni sütununun sayısal olduğundan emin olalım
        const yAxisColMetadata = availableColumns.find(col => col.name === yAxisColumn);
        if (!yAxisColMetadata || !isNumericType(yAxisColMetadata.type)) {
            // Bu kontrol zaten otomatik seçim ve filtreleme ile çoğu durumda yakalanır,
            // ancak manuel seçimde hata durumunu tekrar kontrol etmek iyi bir uygulamadır.
            // Bu noktada hata mesajı göstermek yerine null dönmek ve renderChartComponent'in bunu yönetmesi daha uygun.
            return null; 
        }

        const labels = chartDataRaw.map(row => row[xAxisColumn]);
        const values = chartDataRaw.map(row => row[yAxisColumn]);

        const chartOptions = {
            responsive: true,
            plugins: {
                legend: {
                    position: 'top',
                },
                title: {
                    display: true,
                    text: `${formatLabel(selectedObject)} - ${formatLabel(yAxisColumn)} by ${formatLabel(xAxisColumn)}`,
                },
            },
            scales: {} // Scales objesi burada boş bırakılabilir, Chart.js default ayarlarına göre oluşturur
        };

        // Chart.js veri formatına dönüştür
        return {
            labels: labels,
            datasets: [
                {
                    label: formatLabel(yAxisColumn), // Y ekseninin etiketini formatla
                    data: values,
                    backgroundColor: chartType === 'radar' ? 'rgba(75, 192, 192, 0.2)' : 'rgba(75, 192, 192, 0.6)',
                    borderColor: 'rgba(75, 192, 192, 1)',
                    borderWidth: 1,
                    fill: chartType === 'radar' ? true : false,
                },
            ],
            options: chartOptions // prepareChartData'dan options da dönsün
        };
    };

    // Grafik bileşenini render eden fonksiyon
    const renderChartComponent = () => {
        const preparedData = prepareChartData();
        if (!preparedData) {
            return null;
        }

        const { options, ...dataForChart } = preparedData;

        switch (chartType) {
            case 'bar':
                return <Bar data={dataForChart} options={options} />;
            case 'line':
                return <Line data={dataForChart} options={options} />;
            case 'radar':
                return <Radar data={dataForChart} options={options} />;
            default:
                return <Bar data={dataForChart} options={options} />;
        }
    };

   
    return (
        <div className="chart-configurator">
            <h2>Veritabanı Bağlantı Bilgileri</h2>
            <div className="connection-form">
                <div>
                    <label htmlFor="host">Host:</label>
                    <input type="text" name="host" value={dbConfig.host} onChange={handleDbConfigChange} />
                </div>
                <div>
                    <label htmlFor="dbName">Veritabanı Adı:</label>
                    <input type="text" name="dbName" value={dbConfig.dbName} onChange={handleDbConfigChange} />
                </div>
                <div>
                    <label htmlFor="user">Kullanıcı Adı:</label>
                    <input type="text" name="user" value={dbConfig.user} onChange={handleDbConfigChange} />
                </div>
                <div>
                    <label htmlFor="password">Parola:</label>
                    <input type="password" name="password" value={dbConfig.password} onChange={handleDbConfigChange} />
                </div>
                <button onClick={fetchDbObjects} disabled={loading}>
                    {loading ? 'Yükleniyor...' : 'Objeleri Getir ve Bağlan'}
                </button>
            </div>

            {error && <p className="error-message">{error}</p>}
            {message && <p className="success-message">{message}</p>}

            {dbObjects.length > 0 && (
                <div className="chart-config-form">
                    <h2>Grafik Ayarları</h2>
                    <div>
                        <label htmlFor="selectedObject">Veri Objesi Seç:</label>
                        <select id="selectedObject" value={selectedObject} onChange={(e) => setSelectedObject(e.target.value)}>
                            <option value="">Objeyi Seçiniz</option>
                            {dbObjects.map((obj, index) => (
                                <option key={index} value={obj}>{formatLabel(obj)}</option>
                            ))}
                        </select>
                    </div>

                    {/* Fonksiyon Parametre Girişi */}
                    {selectedObject && objectParameters.length > 0 && (
                        <div>
                            <h3>Parametreler:</h3>
                            {objectParameters.map((param, index) => (
                                <div key={index} className="parameter-input-group">
                                    <label htmlFor={`param-${param.name}`}>{formatLabel(param.name)}:</label> {/* Parametre adını formatla */}
                                    <input
                                        type={param.type && (param.type.includes('int') || param.type.includes('numeric')) ? 'number' : 'text'}
                                        id={`param-${param.name}`}
                                        value={paramValues[param.name] || ''}
                                        onChange={(e) => handleParamValueChange(param.name, e.target.value)}
                                        placeholder={`Lütfen değer giriniz.`}
                                    />
                                </div>
                            ))}
                        </div>
                    )}
                     {selectedObject && objectParameters.length === 0 && !loading && (
                         <p>Seçili obje için parametre bulunamadı veya bu bir View'dır.</p>
                     )}


                    <button onClick={fetchChartData} disabled={loading || !selectedObject}>
                        {loading ? 'Veri Çekiliyor...' : 'Grafik Verilerini Çek'}
                    </button>
                </div>
            )}

            {chartDataRaw && chartDataRaw.length > 0 && availableColumns.length > 0 && (
                <div className="chart-config-form">
                    <h2>Veri Haritalama</h2>
                    <div>
                        <label htmlFor="xAxis">X Ekseni</label>
                        <select id="xAxis" value={xAxisColumn} onChange={(e) => setXAxisColumn(e.target.value)}>
                            <option value="">Seçiniz</option>
                            {availableColumns.map((col, index) => (
                                <option key={index} value={col.name}>{formatLabel(col.name)}</option> 
                            ))}
                        </select>
                    </div>
                    <div>
                        <label htmlFor="yAxis">Y Ekseni</label>
                        <select id="yAxis" value={yAxisColumn} onChange={(e) => setYAxisColumn(e.target.value)}>
                            <option value="">Seçiniz</option>
                            {availableColumns
                                .filter(col => isNumericType(col.type)) // Sadece sayısal tipleri filtrele!
                                .map((col, index) => (
                                  <option key={index} value={col.name}>{formatLabel(col.name)}</option> 
                                ))}
                        </select>
                        {availableColumns.filter(col => !isNumericType(col.type)).length > 0 && (
                            <p className="info-message">Not: Y eksenine sadece sayısal tipler konulabilir.</p>
                        )}
                    </div>
                    <div>
                        <label htmlFor="chartType">Grafik Tipi:</label>
                        <select id="chartType" value={chartType} onChange={(e) => setChartType(e.target.value)}>
                            <option value="bar">Bar (Çubuk)</option>
                            <option value="line">Line (Çizgi)</option>
                            <option value="radar">Radar</option>
                            {/* İleride diğer tipleri buraya ekleyebilirsiniz */}
                        </select>
                    </div>
                </div>
            )}

            {/* Grafik Gösterimi */}
            {chartDataRaw && chartDataRaw.length > 0 && xAxisColumn && yAxisColumn && (
                <div className="chart-container">
                    {renderChartComponent()}
                </div>
            )}
            {/* Veri yoksa veya uygun değilse mesaj göster */}
            {chartDataRaw && chartDataRaw.length === 0 && <p>Çekilen veri bulunamadı veya boş.</p>}
            {chartDataRaw && availableColumns.length > 0 && (!xAxisColumn || !yAxisColumn) && (
                <p>Lütfen X ve Y eksenlerini seçin.</p>
            )}
        </div>
    );
}

export default ChartConfigurator;