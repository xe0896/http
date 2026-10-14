SRC_DIR := src
TEST_DIR := tests
OUT_DIR := out
LIB_DIR := lib
MAIN := Main

SOURCES := $(shell find $(SRC_DIR) -name "*.java")
TEST_SOURCES := $(shell find $(TEST_DIR) -name "*.java")
CLASSPATH := $(OUT_DIR):$(LIB_DIR)/*

.PHONY: compile run test clean

compile:
	@mkdir -p $(OUT_DIR)
	@javac -cp "$(CLASSPATH)" -d $(OUT_DIR) $(SOURCES)

run: compile
	@java -cp "$(CLASSPATH)" $(MAIN)

test: compile
	@javac -cp "$(CLASSPATH)" -d $(OUT_DIR) $(TEST_SOURCES)
	@java -jar $(LIB_DIR)/junit.jar execute --class-path $(OUT_DIR) --scan-class-path

clean:
	@rm -rf $(OUT_DIR)