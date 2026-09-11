-- ============================================================================
-- DriveAI — V2: Datos semilla del catálogo (25 vehículos)
-- Precios en USD, kilometraje en km, consumo en L/100km (kWh/100km para
-- eléctricos). IDs explícitos para referencias estables en demos y tests.
-- Imágenes: URLs estables de Unsplash (auto=format&fit=crop&w=1200&q=80).
-- ============================================================================

INSERT INTO cars (id, brand, model, year, price, mileage, fuel_type, fuel_consumption,
                  transmission, body_type, image_url, description, stock)
VALUES
-- ============================ HATCHBACK ============================
(1, 'Mazda', '3 Sport', 2022, 24200.00, 15500, 'GASOLINA', 6.00, 'AUTOMATICA', 'HATCHBACK',
 'https://images.unsplash.com/photo-1553440569-bcc63803a83d?auto=format&fit=crop&w=1200&q=80',
 'Hatchback compacto con excelente comportamiento dinámico, buena conectividad y consumos contenidos.', 5),

(2, 'Volkswagen', 'Golf 1.4 TSI', 2023, 28900.00, 6500, 'GASOLINA', 5.90, 'AUTOMATICA', 'HATCHBACK',
 'https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?auto=format&fit=crop&w=1200&q=80',
 'Referencia del segmento: calidad de conducción superior, interior sobrio y mecánica eficiente.', 2),

(3, 'Kia', 'Picanto', 2021, 14500.00, 28000, 'GASOLINA', 5.20, 'MANUAL', 'HATCHBACK',
 'https://images.unsplash.com/photo-1533416784636-2b0ccfea6c97?auto=format&fit=crop&w=1200&q=80',
 'City car urbano, económico de mantener y perfecto para desplazamientos diarios en ciudad.', 6),

(4, 'Chevrolet', 'Onix', 2022, 18200.00, 19000, 'GASOLINA', 6.10, 'AUTOMATICA', 'HATCHBACK',
 'https://images.unsplash.com/photo-1606016159991-dfe4f2746ad5?auto=format&fit=crop&w=1200&q=80',
 'Compacto con pantalla táctil, buen equipamiento de seguridad y excelente relación precio-valor.', 7),

(5, 'Suzuki', 'Swift GL', 2022, 16500.00, 16000, 'GASOLINA', 5.60, 'MANUAL', 'HATCHBACK',
 'https://images.unsplash.com/photo-1583121274602-3e2820c69888?auto=format&fit=crop&w=1200&q=80',
 'Ágil, liviano y de bajo consumo. Ideal para quien busca un usado confiable con costos mínimos.', 5),

(6, 'Nissan', 'Leaf', 2022, 23800.00, 16000, 'ELECTRICO', 17.50, 'AUTOMATICA', 'HATCHBACK',
 'https://images.unsplash.com/photo-1560958089-b8a1929cea89?auto=format&fit=crop&w=1200&q=80',
 'Eléctrico de acceso con 100% de par disponible y costo por kilómetro muy bajo.', 4),

-- ============================ SEDAN ============================
(7, 'Toyota', 'Corolla', 2022, 22500.00, 12000, 'GASOLINA', 5.80, 'AUTOMATICA', 'SEDAN',
 'https://images.unsplash.com/photo-1600718374662-0483d2b9da44?auto=format&fit=crop&w=1200&q=80',
 'El sedán más confiable del mercado. Historial de mantenimiento impecable y reventa garantizada.', 4),

(8, 'Honda', 'Civic Touring', 2023, 26800.00, 8000, 'GASOLINA', 6.20, 'AUTOMATICA', 'SEDAN',
 'https://images.unsplash.com/photo-1606664515524-ed2f786a0bd6?auto=format&fit=crop&w=1200&q=80',
 'Sedán con espíritu deportivo, ADAS Honda Sensing y calidad percibida superior.', 3),

(9, 'Volkswagen', 'Jetta 1.4 TSI', 2022, 23500.00, 21000, 'GASOLINA', 6.40, 'AUTOMATICA', 'SEDAN',
 'https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=1200&q=80',
 'Auto estable en ruta, baúl amplio y mecánica turbo de respuesta ágil.', 5),

(10, 'Tesla', 'Model 3', 2023, 38900.00, 9000, 'ELECTRICO', 15.00, 'AUTOMATICA', 'SEDAN',
 'https://images.unsplash.com/photo-1560958089-b8a1929cea89?auto=format&fit=crop&w=1200&q=80',
 'Eléctrico de referencia: alto rango, supercargadores y aceleración instantánea.', 3),

(11, 'BMW', '330i M-Sport', 2022, 41500.00, 14200, 'GASOLINA', 7.40, 'AUTOMATICA', 'SEDAN',
 'https://images.unsplash.com/photo-1555215695-3004980ad54e?auto=format&fit=crop&w=1200&q=80',
 'Berlina deportiva con dinámica de conducción líder del segmento y acabados premium.', 2),

(12, 'Mercedes-Benz', 'C300 AMG Line', 2023, 48500.00, 8800, 'GASOLINA', 7.90, 'AUTOMATICA', 'SEDAN',
 'https://images.unsplash.com/photo-1542362567-b07e54358753?auto=format&fit=crop&w=1200&q=80',
 'Lujo y confort de marcha excepcionales con una pantalla central MBUX de 11.9 pulgadas.', 2),

(13, 'Audi', 'A3 35 TFSI', 2022, 36500.00, 12000, 'GASOLINA', 6.60, 'AUTOMATICA', 'SEDAN',
 'https://images.unsplash.com/photo-1580273916550-e323be2ae537?auto=format&fit=crop&w=1200&q=80',
 'Calidad constructiva y minimalismo tecnológico con Virtual Cockpit de serie.', 3),

-- ============================ SUV ============================
(14, 'Toyota', 'RAV4 Hybrid', 2024, 35200.00, 5000, 'HIBRIDO', 5.10, 'AUTOMATICA', 'SUV',
 'https://images.unsplash.com/photo-1605559424843-9e4c228bf1c2?auto=format&fit=crop&w=1200&q=80',
 'SUV híbrida que combina espacio familiar con un consumo de ciudad casi de compacto.', 5),

(15, 'Hyundai', 'Tucson Hybrid', 2023, 33900.00, 9500, 'HIBRIDO', 5.40, 'AUTOMATICA', 'SUV',
 'https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?auto=format&fit=crop&w=1200&q=80',
 'Diseño vanguardista, maletero generoso y tecnología híbrida de bajo consumo.', 4),

(16, 'Honda', 'CR-V e:HEV', 2023, 36800.00, 11000, 'HIBRIDO', 6.00, 'AUTOMATICA', 'SUV',
 'https://images.unsplash.com/photo-1511919884226-fd3cad34687c?auto=format&fit=crop&w=1200&q=80',
 'SUV familiar de referencia: amplia, segura (Honda Sensing) y muy eficiente.', 3),

(17, 'Kia', 'Sportage GT-Line', 2023, 27400.00, 13000, 'GASOLINA', 7.20, 'AUTOMATICA', 'SUV',
 'https://images.unsplash.com/photo-1553440569-bcc63803a83d?auto=format&fit=crop&w=1200&q=80',
 'Estética audaz, excelente garantía 7 años y buena relación precio-equipamiento.', 4),

(18, 'Ford', 'Escape SE', 2022, 25900.00, 21000, 'GASOLINA', 7.60, 'AUTOMATICA', 'SUV',
 'https://images.unsplash.com/photo-1494976388531-d1058494cdd8?auto=format&fit=crop&w=1200&q=80',
 'SUV cómoda para viajar, con maletero espacioso y tecnología Co-Pilot360.', 3),

(19, 'Nissan', 'Kicks Exclusive', 2022, 21800.00, 18000, 'GASOLINA', 6.80, 'AUTOMATICA', 'SUV',
 'https://images.unsplash.com/photo-1606664515524-ed2f786a0bd6?auto=format&fit=crop&w=1200&q=80',
 'SUV urbana económica, con cámara 360° y pantalla flotante de 8 pulgadas.', 6),

(20, 'Volkswagen', 'Taos Comfortline', 2022, 27900.00, 17500, 'GASOLINA', 7.10, 'AUTOMATICA', 'SUV',
 'https://images.unsplash.com/photo-1606015960883-4a6dd942843d?auto=format&fit=crop&w=1200&q=80',
 'SUV compacta con buen comportamiento en ruta y espacio para la familia.', 4),

(21, 'Tesla', 'Model Y', 2024, 44900.00, 7000, 'ELECTRICO', 16.20, 'AUTOMATICA', 'SUV',
 'https://images.unsplash.com/photo-1617788138017-80ad40651399?auto=format&fit=crop&w=1200&q=80',
 'SUV eléctrica espaciosa con techo panorámico, gran autonomía y actualizaciones inalámbricas.', 2),

(22, 'Hyundai', 'IONIQ 5', 2023, 41500.00, 10000, 'ELECTRICO', 16.80, 'AUTOMATICA', 'SUV',
 'https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?auto=format&fit=crop&w=1200&q=80',
 'Eléctrico futurista de carga ultrarrápida (350 kW) y habitabilidad sorprendente.', 2),

-- ============================ PICKUP ============================
(23, 'Toyota', 'Hilux SRX 4x4', 2022, 38500.00, 24000, 'DIESEL', 7.80, 'AUTOMATICA', 'PICKUP',
 'https://images.unsplash.com/photo-1601362840469-51e4d8a58785?auto=format&fit=crop&w=1200&q=80',
 'Camioneta más confiable de su clase: robusta, tracción 4x4 y enorme capacidad de carga.', 3),

(24, 'Ford', 'Ranger XLT', 2023, 41200.00, 15000, 'DIESEL', 8.10, 'AUTOMATICA', 'PICKUP',
 'https://images.unsplash.com/photo-1617531653332-bd46c24f2068?auto=format&fit=crop&w=1200&q=80',
 'Pickup moderna con torque diesel generoso, off-road capaz y conjunto SYNC de serie.', 2),

(25, 'Chevrolet', 'Silverado 1500', 2021, 45500.00, 31000, 'GASOLINA', 11.20, 'AUTOMATICA', 'PICKUP',
 'https://images.unsplash.com/photo-1556229010-6c3f2c9ca5f8?auto=format&fit=crop&w=1200&q=80',
 'Full-size pickup americana con caja de 1.80 m, remolque de hasta 4,5 toneladas y mucho confort.', 2);