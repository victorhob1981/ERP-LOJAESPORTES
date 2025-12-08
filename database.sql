-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: gemini_erp
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `clientes`
--

DROP TABLE IF EXISTS `clientes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `clientes` (
  `ClienteID` int NOT NULL AUTO_INCREMENT,
  `NomeCliente` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ContatoCliente` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ClienteID`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `clientes`
--

LOCK TABLES `clientes` WRITE;
/*!40000 ALTER TABLE `clientes` DISABLE KEYS */;
INSERT INTO `clientes` VALUES 
(1,'Cliente Consumidor','(21) 90000-0000'),
(2,'João da Silva','(21) 99999-1111'),
(3,'Maria Oliveira','(21) 98888-2222'),
(4,'Carlos Souza','(21) 97777-3333'),
(5,'Ana Pereira','(21) 96666-4444');
/*!40000 ALTER TABLE `clientes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `produtos`
--

DROP TABLE IF EXISTS `produtos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `produtos` (
  `ProdutoID` int NOT NULL AUTO_INCREMENT,
  `Modelo` varchar(150) NOT NULL,
  `Clube` varchar(150) NOT NULL,
  `Tipo` enum('Masculina','Feminina','Infantil') NOT NULL,
  `Tamanho` varchar(20) NOT NULL,
  `DescricaoCompleta` varchar(500) DEFAULT NULL,
  `PrecoVendaAtual` decimal(10,2) NOT NULL DEFAULT '0.00',
  `QuantidadeEstoque` int NOT NULL DEFAULT '0',
  `CustoMedioPonderado` decimal(10,2) NOT NULL DEFAULT '0.00',
  `DataCadastro` datetime DEFAULT CURRENT_TIMESTAMP,
  `DataUltimaEntradaEstoque` datetime DEFAULT NULL,
  `CaminhoImagem` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`ProdutoID`),
  UNIQUE KEY `idx_produto_unico` (`Modelo`,`Clube`,`Tipo`,`Tamanho`),
  UNIQUE KEY `uq_produto` (`Clube`,`Modelo`,`Tipo`,`Tamanho`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `produtos`
--

LOCK TABLES `produtos` WRITE;
/*!40000 ALTER TABLE `produtos` DISABLE KEYS */;
INSERT INTO `produtos` VALUES 
(1,'HOME 2025','FLAMENGO','Masculina','G',NULL,150.00,10,80.00,NOW(),NOW(),NULL),
(2,'HOME 2025','FLAMENGO','Masculina','M',NULL,150.00,5,80.00,NOW(),NOW(),NULL),
(3,'AWAY 2025','VASCO','Masculina','GG',NULL,140.00,8,75.00,NOW(),NOW(),NULL),
(4,'THIRD 2025','BOTAFOGO','Masculina','G',NULL,160.00,3,85.00,NOW(),NOW(),NULL),
(5,'TRICOLOR','FLUMINENSE','Feminina','P',NULL,130.00,12,65.00,NOW(),NOW(),NULL),
(6,'SELECAO BRASIL','BRASIL','Infantil','10',NULL,120.00,20,60.00,NOW(),NOW(),NULL),
(7,'HOME 2024','REAL MADRID','Masculina','G',NULL,180.00,4,90.00,NOW(),NOW(),NULL),
(8,'HOME 2024','MAN CITY','Masculina','M',NULL,180.00,2,90.00,NOW(),NOW(),NULL),
(9,'RETRO 1981','FLAMENGO','Masculina','G',NULL,200.00,1,100.00,NOW(),NOW(),NULL),
(10,'TREINO 2025','VASCO','Masculina','GG',NULL,110.00,6,50.00,NOW(),NOW(),NULL);
/*!40000 ALTER TABLE `produtos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `encomendascliente`
--

DROP TABLE IF EXISTS `encomendascliente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `encomendascliente` (
  `EncomendaClienteID` int NOT NULL AUTO_INCREMENT,
  `ClienteID` int NOT NULL,
  `DataEncomenda` date DEFAULT (curdate()),
  `Clube` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Modelo` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Tipo` enum('Masculina','Feminina','Infantil') COLLATE utf8mb4_unicode_ci NOT NULL,
  `Tamanho` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Observacao` text COLLATE utf8mb4_unicode_ci,
  `StatusEncomenda` enum('Pendente','PedidoAoFornecedorFeito','ProdutoChegou','EntregueAoCliente','Cancelada') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Pendente',
  `ProdutoIDAssociado` int DEFAULT NULL,
  PRIMARY KEY (`EncomendaClienteID`),
  KEY `ClienteID` (`ClienteID`),
  KEY `ProdutoIDAssociado` (`ProdutoIDAssociado`),
  CONSTRAINT `encomendascliente_ibfk_1` FOREIGN KEY (`ClienteID`) REFERENCES `clientes` (`ClienteID`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `encomendascliente_ibfk_2` FOREIGN KEY (`ProdutoIDAssociado`) REFERENCES `produtos` (`ProdutoID`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `encomendascliente`
--

LOCK TABLES `encomendascliente` WRITE;
/*!40000 ALTER TABLE `encomendascliente` DISABLE KEYS */;
INSERT INTO `encomendascliente` VALUES 
(1,2,'2025-10-01','FLAMENGO','FINAL LIBERTADORES','Masculina','G','Personalizar com Zico 10','Pendente',NULL),
(2,3,'2025-10-05','VASCO','CAMISA NEGRA','Feminina','M',NULL,'EntregueAoCliente',3),
(3,4,'2025-10-10','PSG','JORDAN','Masculina','GG',NULL,'PedidoAoFornecedorFeito',NULL);
/*!40000 ALTER TABLE `encomendascliente` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pedidosfornecedor`
--

DROP TABLE IF EXISTS `pedidosfornecedor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pedidosfornecedor` (
  `PedidoFornecedorID` int NOT NULL AUTO_INCREMENT,
  `DataPedido` date DEFAULT (curdate()),
  `NomeFornecedor` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CustoTotalEstimadoItens` decimal(10,2) NOT NULL DEFAULT '0.00',
  `TaxaImportacaoTotal` decimal(10,2) NOT NULL DEFAULT '0.00',
  `CustoTotalFinalPedido` decimal(10,2) NOT NULL DEFAULT '0.00',
  `StatusPedido` enum('Realizado','Recebido Parcialmente','Recebido Integralmente') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Realizado',
  PRIMARY KEY (`PedidoFornecedorID`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pedidosfornecedor`
--

LOCK TABLES `pedidosfornecedor` WRITE;
/*!40000 ALTER TABLE `pedidosfornecedor` DISABLE KEYS */;
INSERT INTO `pedidosfornecedor` VALUES 
(1,'2025-09-01','Fornecedor A',800.00,0.00,800.00,'Recebido Integralmente'),
(2,'2025-09-15','Fornecedor Express',450.00,50.00,500.00,'Recebido Parcialmente');
/*!40000 ALTER TABLE `pedidosfornecedor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `itenspedidofornecedor`
--

DROP TABLE IF EXISTS `itenspedidofornecedor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `itenspedidofornecedor` (
  `ItemPedidoFornecedorID` int NOT NULL AUTO_INCREMENT,
  `PedidoFornecedorID` int NOT NULL,
  `ProdutoID` int NOT NULL,
  `QuantidadePedida` int NOT NULL,
  `CustoUnitarioFornecedor` decimal(10,2) NOT NULL,
  `CustoUnitarioComTaxas` decimal(10,2) DEFAULT NULL,
  `QuantidadeRecebida` int NOT NULL DEFAULT '0',
  `DataRecebimento` date DEFAULT NULL,
  `Chegou` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ItemPedidoFornecedorID`),
  KEY `PedidoFornecedorID` (`PedidoFornecedorID`),
  KEY `ProdutoID` (`ProdutoID`),
  CONSTRAINT `itenspedidofornecedor_ibfk_1` FOREIGN KEY (`PedidoFornecedorID`) REFERENCES `pedidosfornecedor` (`PedidoFornecedorID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `itenspedidofornecedor_ibfk_2` FOREIGN KEY (`ProdutoID`) REFERENCES `produtos` (`ProdutoID`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `itenspedidofornecedor_chk_1` CHECK ((`QuantidadePedida` > 0)),
  CONSTRAINT `itenspedidofornecedor_chk_2` CHECK ((`QuantidadeRecebida` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `itenspedidofornecedor`
--

LOCK TABLES `itenspedidofornecedor` WRITE;
/*!40000 ALTER TABLE `itenspedidofornecedor` DISABLE KEYS */;
INSERT INTO `itenspedidofornecedor` VALUES 
(1,1,1,10,80.00,80.00,10,'2025-09-10',1),
(2,1,2,5,80.00,80.00,5,'2025-09-10',1),
(3,2,3,5,75.00,85.00,5,'2025-09-25',1),
(4,2,4,2,85.00,95.00,0,NULL,0);
/*!40000 ALTER TABLE `itenspedidofornecedor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vendas`
--

DROP TABLE IF EXISTS `vendas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vendas` (
  `VendaID` int NOT NULL AUTO_INCREMENT,
  `ClienteID` int DEFAULT NULL,
  `DataVenda` datetime DEFAULT CURRENT_TIMESTAMP,
  `ValorTotalItens` decimal(10,2) NOT NULL DEFAULT '0.00',
  `ValorDesconto` decimal(10,2) NOT NULL DEFAULT '0.00',
  `ValorFinalVenda` decimal(10,2) NOT NULL DEFAULT '0.00',
  `StatusPagamento` enum('Pago','Pendente') COLLATE utf8mb4_unicode_ci NOT NULL,
  `DataPrometidaPagamento` date DEFAULT NULL,
  `MetodoPagamento` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ValorPago` decimal(10,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`VendaID`),
  KEY `ClienteID` (`ClienteID`),
  CONSTRAINT `vendas_ibfk_1` FOREIGN KEY (`ClienteID`) REFERENCES `clientes` (`ClienteID`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vendas`
--

LOCK TABLES `vendas` WRITE;
/*!40000 ALTER TABLE `vendas` DISABLE KEYS */;
INSERT INTO `vendas` VALUES 
(1,2,NOW(),300.00,10.00,290.00,'Pago',NULL,'Pix',290.00),
(2,3,NOW(),140.00,0.00,140.00,'Pago',NULL,'Dinheiro',140.00),
(3,4,NOW(),480.00,0.00,480.00,'Pendente','2025-12-25','Cartão de Crédito',0.00);
/*!40000 ALTER TABLE `vendas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `itensvenda`
--

DROP TABLE IF EXISTS `itensvenda`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `itensvenda` (
  `ItemVendaID` int NOT NULL AUTO_INCREMENT,
  `VendaID` int NOT NULL,
  `ProdutoID` int NOT NULL,
  `Quantidade` int NOT NULL,
  `PrecoVendaUnitarioRegistrado` decimal(10,2) NOT NULL,
  `CustoMedioUnitarioRegistrado` decimal(10,2) NOT NULL,
  PRIMARY KEY (`ItemVendaID`),
  KEY `VendaID` (`VendaID`),
  KEY `ProdutoID` (`ProdutoID`),
  CONSTRAINT `itensvenda_ibfk_1` FOREIGN KEY (`VendaID`) REFERENCES `vendas` (`VendaID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `itensvenda_ibfk_2` FOREIGN KEY (`ProdutoID`) REFERENCES `produtos` (`ProdutoID`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `itensvenda_chk_1` CHECK ((`Quantidade` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `itensvenda`
--

LOCK TABLES `itensvenda` WRITE;
/*!40000 ALTER TABLE `itensvenda` DISABLE KEYS */;
INSERT INTO `itensvenda` VALUES 
(1,1,1,1,150.00,80.00),
(2,1,2,1,150.00,80.00),
(3,2,3,1,140.00,75.00),
(4,3,4,3,160.00,85.00);
/*!40000 ALTER TABLE `itensvenda` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vendas_temp`
--

DROP TABLE IF EXISTS `vendas_temp`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vendas_temp` (
  `DataVenda` varchar(255) DEFAULT NULL,
  `ID_Venda_Original` int DEFAULT NULL,
  `Produto` varchar(255) DEFAULT NULL,
  `Preco_Unitario` varchar(255) DEFAULT NULL,
  `Pagamento` varchar(255) DEFAULT NULL,
  `Tamanho` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `Quantidade` int DEFAULT NULL,
  `Desconto` varchar(255) DEFAULT NULL,
  `Metodo_Pagamento` varchar(255) DEFAULT NULL,
  `clube_temp` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `modelo_temp` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `tipo_temp` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vendas_temp`
--

LOCK TABLES `vendas_temp` WRITE;
/*!40000 ALTER TABLE `vendas_temp` DISABLE KEYS */;
INSERT INTO `vendas_temp` VALUES 
('01/01/2025',1,'FLAMENGO HOME 2025',' R$ 150,00 ','PAGO','G',1,'0','PIX','FLAMENGO','HOME 2025','Masculina'),
('02/01/2025',2,'VASCO AWAY 2025',' R$ 140,00 ','PAGO','GG',1,'0','DINHEIRO','VASCO','AWAY 2025','Masculina');
/*!40000 ALTER TABLE `vendas_temp` ENABLE KEYS */;
UNLOCK TABLES;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;