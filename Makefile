SRC_DIR := src
OUT_DIR := out
LIB_DIR := lib
MAIN := Main

SOURCES := $(shell find $(SRC_DIR) -name "*.java")
CLASSPATH := $(OUT_DIR):$(LIB_DIR)/*

.PHONY: compile run clean

compile:
	@mkdir -p $(OUT_DIR) $(LIB_DIR)
	@javac -cp "$(LIB_DIR)/*" -d $(OUT_DIR) $(SOURCES)

run: compile
	@java -cp "$(CLASSPATH)" $(MAIN)

clean:
	@rm -rf $(OUT_DIR)